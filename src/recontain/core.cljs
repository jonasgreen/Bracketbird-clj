(ns recontain.core
  (:refer-clojure :exclude [update])
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [recontain.setup :as s]))


(defonce component-states-atom (atom {}))
(defonce config-atom (atom {}))


(defn setup [config]
  (reset! component-states-atom {})
  (reset! config-atom (s/resolve-config config))
  @config-atom)

(defn- state-atom [] (:state-atom @config-atom))

(defn- debug [f]
  (when (:debug? @config-atom) (f)))

(defn- get-hook-value [hook]
  (let [hook-value (get-in @config-atom [:hooks hook])]
    (when-not hook-value (throw (js/Error. (str "Unable to find mapping fog hook: " hook " in config"))))
    hook-value))

(defn- ui-hook? [hook]
  (s/ui-hook? (get-hook-value hook)))

(defn- validate [hook given-ctx]
  (let [rq-ctx (:ctx (get-hook-value hook))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]

    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for hook " hook ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn- resolve-path-from-ctx [hook given-ctx]
  (let [{:keys [path ctx]} (get-hook-value hook)]
    (reduce (fn [v p]
              (if (set? p)
                (if-let [ctx-value (get given-ctx (first p))]
                  (conj v ctx-value)
                  (throw (js/Error. (str "Missing context " p " for hook " hook ". Given ctx: " given-ctx ". Required ctx: " ctx))))
                (conj v p)))
            []
            path)))

(defn hook-path [hook ctx]
  (validate hook ctx)
  (let [p (resolve-path-from-ctx hook ctx)]
    (if (ui-hook? hook)
      (conj p :_local-state)
      p)))

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


(defn mk-id [ctx hook]
  (hash (hook-path hook ctx)))

;;;; API


(defn build
  ([ctx hook]
   (build ctx hook {}))

  ([ctx hook opts]
   (validate hook ctx)
   [gui ctx hook opts]))


(defn- get-handle-data
  ([ctx hook]
   (get-handle-data (mk-id ctx hook)))
  ([id]
   (get @component-states-atom id)))

(defn id
  ([handle] (:id handle))
  ([handle sub-id] (str (:id handle) sub-id)))

(defn update [state {:keys [id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-handle-data id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @config-atom) #(apply update % handle args)))

(defn dispatch [handle dispatch-f & args]
  (let [{:keys [hook id]} handle
        h-data (get-handle-data id)
        f (-> @config-atom
              (get-in [:hooks hook])
              (get dispatch-f))]
    (when-not f (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply f (:handle h-data) (:local-state h-data) (:foreign-states h-data) args)))

(defn get-element
  ([handle] (-> handle :id dom/getElement))
  ([handle sub-id] (-> handle (id sub-id) dom/getElement)))

(defn get-handle [ctx hook]
  (validate hook ctx)
  (:handle (get-handle-data ctx hook)))

(defn get-data [handle hook]
  (get-in @(state-atom) (hook-path hook (:ctx handle))))

(defn ctx
  ([handle] (:ctx handle))
  ([handle extra-ctx] (merge (:ctx handle) extra-ctx)))
