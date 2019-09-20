(ns recontain.core
  (:refer-clojure :exclude [update])
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [recontain.setup :as s]))


(defonce component-states-atom (atom {}))
(defonce config-atom (atom {}))


(defn setup [config]
  ;todo - different validations, data refinements etc
  (reset! component-states-atom {})
  (reset! config-atom (s/resolve-config config))
  @config-atom)

(defn- state-atom [] (:state-atom @config-atom))

(defn- debug [f]
  (when (:debug? @config-atom) (f)))

(defn- resolve-path-from-ctx [hooks hook ctx]
  (let [path (get-in hooks [hook :path])
        config-ctx (get-in hooks [hook :ctx])]
    (when (nil? path)
      (throw (js/Error. (str "Unable to find mapping for hook " hook " in config"))))

    (reduce (fn [v p]
              (if (set? p)
                (if-let [ctx-value (get ctx (first p))]
                  (conj v ctx-value)
                  (throw (js/Error. (str "Unable to find ctx-value for " p . " Given ctx: " ctx ". Required ctx: " config-ctx))))
                (conj v p)))
            []
            path)))


(defn hook-path [hook ctx]
  (resolve-path-from-ctx (:hooks @config-atom) hook ctx))

(defn- resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (get initial-values hook)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))


(defn- gui [ctx hook _]
  (let [state-atom (get @config-atom :state-atom)
        ui-container (get-in @config-atom [:hooks hook])
        all-hooks (into [hook] (:reactions ui-container))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        id (hash (get all-paths hook))
        path (get all-paths hook)

        ;should not be reaction dependent
        initial-values (reduce (fn [m h] (assoc m h (get-in @config-atom [:hooks h :local-state] {})))
                               {}
                               all-hooks)

        ; includes state and foreign state
        reactions-map (reduce (fn [m h] (assoc m h (reaction (get-in @state-atom (h all-paths) nil)))) {} all-hooks)

        handle {:id            id
                :ctx           ctx
                :path          path
                :hook          hook
                :foreign-paths (-> all-paths
                                   (dissoc hook)
                                   vals
                                   vec)}]

    (r/create-class
      {:component-did-mount (fn [_]
                              (debug #(println "DID MOUNT - " hook))
                              (when-let [dm (:did-mount ui-container)]
                                (let [{:keys [local-state foreign-states]} (get @component-states-atom id)]
                                  (dm handle local-state foreign-states))))
       :reagent-render      (fn [_ _ opts]
                              (debug #(println "RENDER - " hook))
                              (let [
                                    ;dereferences and initializes
                                    state-map (resolve-reactions reactions-map initial-values)

                                    local-state (get state-map hook)
                                    foreign-states (dissoc state-map hook)

                                    ;options

                                    _ (swap! component-states-atom assoc id {:handle         handle
                                                                             :local-state    local-state
                                                                             :foreign-states foreign-states})
                                    render (:render ui-container)]

                                (if-not render
                                  [:div (str "No render: " hook ctx)]

                                  (let [rendered (render handle local-state foreign-states opts) ;instead of reagent calling render function - we do it
                                        [elm elm-opts & remaining] rendered

                                        ;insert dom-id
                                        result (into (if (map? elm-opts)
                                                       [elm (assoc elm-opts :id id)]
                                                       [elm {:id id} elm-opts]) remaining)]



                                    (if-let [decorator (:component-hiccup-decorator @config-atom)]
                                      (decorator result (get @component-states-atom id))
                                      result)))))})))
(defn- mk-handle-id [ctx hook]
  (hash (hook-path hook ctx)))

;;;; API

(defn build [{:keys [ctx]} hook next-ctx opts]
  [gui (merge ctx next-ctx) hook opts])

(defn ui-root [hook]
  (build {:ctx {}} hook {} nil))

(defn- get-handle-data
  ([ctx hook]
   (get-handle-data (mk-handle-id ctx hook)))
  ([id]
   (get @component-states-atom id)))


(defn mk-id [handle sub-id] (str (:id handle) sub-id))

(defn update [state {:keys [id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-handle-data id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @config-atom) #(apply update % handle args)))

(defn dispatch [{:keys [hook id] :as handle} dispatch-f & args]
  (let [h-data (get-handle-data id)
        f (-> @config-atom
              (get-in [:hooks hook])
              (get dispatch-f))]
    (when-not f (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply f (:handle h-data) (:local-state h-data) (:foreign-states h-data) args)))

(defn get-element
  ([handle] (-> handle :id dom/getElement))
  ([handle sub-id] (-> handle (mk-id sub-id) dom/getElement)))

(defn get-handle
  ([handle hook] (get-handle handle hook {}))
  ([{:keys [ctx]} hook additional-ctx]
   (:handle (get-handle-data (merge ctx additional-ctx) hook))))