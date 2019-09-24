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

(defn- validate-ctx [hook given-ctx]
  (let [rq-ctx (:ctx (get-hook-value hook))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]

    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for hook " hook ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn- validate-layout [parent-handle hook])

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
  (validate-ctx hook ctx)
  (let [p (resolve-path-from-ctx hook ctx)]
    (if (ui-hook? hook)
      (conj p :_local-state)
      p)))

(defn- resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (get initial-values hook)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))

(defn- gui [parent ctx hook _]
  (let [state-atom (get @config-atom :state-atom)
        ui-container (get-in @config-atom [:hooks hook])
        all-hooks (into [hook] (:reactions ui-container))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        foreign-local-state-ids (->> (dissoc all-paths hook)
                                     keys
                                     (filter ui-hook?)
                                     (select-keys all-paths)
                                     (reduce-kv (fn [m k v] (assoc m k (hash v))) {}))

        id (hash (get all-paths hook))
        path (get all-paths hook)

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
       :reagent-render      (fn [_ _ _ opts]
                              (debug #(println "RENDER - " hook))
                              (let [
                                    ;dereferences values from state atom
                                    state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} reactions-map)


                                    ;handle foreign states first - because they are passed to local-state-fn

                                    ;; foreign local states are found in component cache - because they have been initialized by renderings
                                    ;; higher up the tree
                                    foreign-local-states (reduce-kv (fn [m k v] (assoc m k (get-in @component-states-atom [v :local-state])))
                                                                    {}
                                                                    foreign-local-state-ids)

                                    foreign-states (-> state-map
                                                       (dissoc hook)
                                                       (merge foreign-local-states))

                                    local-state (if-let [ls (get state-map hook)]
                                                  ls
                                                  (if-let [ls-fn (get-in @config-atom [:hooks hook :local-state])]
                                                    (if (map? ls-fn)
                                                      ls-fn
                                                      (ls-fn foreign-states))
                                                    {}))

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
  ([handle extra-ctx hook]
   (build handle extra-ctx hook {}))

  ([handle extra-ctx hook opts]
   (let [ctx (merge (:ctx handle) extra-ctx)
         hook-key (if (fn? hook) (get (:render-to-hook @config-atom) hook) hook)
         ]
     (validate-ctx hook-key ctx)
     (validate-layout handle hook)
     [gui handle ctx hook-key opts])))


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

(defn delete-local-state [handle]
  (let [{:keys [id path]} handle
        parent-path (subvec path 0 (- (count path) 1))]
    (println "parent path" parent-path)
    (swap! component-states-atom update-in [id] dissoc :local-state)
    (swap! (:state-atom @config-atom) update-in parent-path dissoc :_local-state)))

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
  (validate-ctx hook ctx)
  (:handle (get-handle-data ctx hook)))

(defn get-data
  ([handle hook]
   (get-data handle {} hook))
  ([handle extra-ctx hook]
   (get-in @(state-atom) (hook-path hook (merge (:ctx handle) extra-ctx)))))

(defn ctx
  ([handle] (:ctx handle))
  ([handle extra-ctx] (merge (:ctx handle) extra-ctx)))
