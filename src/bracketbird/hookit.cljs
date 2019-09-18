(ns bracketbird.hookit
  (:refer-clojure :exclude [update get])
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [goog.dom :as dom]
            [reagent.core :as r]))

(declare gui hook-path get-handle)

(defonce component-states (atom {}))

(def core-get cljs.core/get)


(defn- resolve-path [hooks hook]
  {:pre [(keyword? hook)]}
  (let [hv (core-get hooks hook)
        p (if (ut/ui-hook? hook) (:path hv) hv)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " hook " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn hook-path [hook ctx]
  (let [path (resolve-path (:hooks @state/state) hook)]

    (->> (if (ut/ui-hook? hook) (conj path :_local-state) path)
         ;replace id's
         (map (fn [p] (core-get ctx p p)))
         (vec))))

(defn insert-debug-info [result {:keys [handle local-state foreign-states]}]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [_]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:hook handle)))
                                             (println (str "RENDER RESULT\n" (b-ut/pp-str result)))
                                             (println "HANDLE\n" (b-ut/pp-str handle))
                                             (println "LOCAL-STATE\n" (b-ut/pp-str local-state))
                                             (println "FOREIGN-STATE-KEYS\n" (b-ut/pp-str (keys foreign-states))))}
                        (:hook handle)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))

(defn resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (core-get initial-values hook)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))


(defn gui [ctx hook _]
  {:pre [(keyword? hook) (map? ctx)]}
  #_(println "NEW GUI - " hook)
  (let [system (state/subscribe [:system] ctx)

        r (get-in @state/state [:hooks hook])
        all-hooks (into [hook] (:reactions r))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        id (hash (core-get all-paths hook))
        path (core-get all-paths hook)

        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:hooks hook :local-state] {})
                                              {})))
                               {}
                               all-hooks)

        ; includes state and foreign state
        reactions-map (reduce (fn [m h] (assoc m h (state/subscribe (h all-paths)))) {} all-hooks)

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
                              (when (:debug? @system) (println "DID MOUNT - " hook))
                              (when-let [dm (:did-mount r)]
                                (let [{:keys [local-state foreign-states]} (core-get @component-states id)]
                                  (dm handle local-state foreign-states))))
       :reagent-render      (fn [_ _ opts]
                              (let [{:keys [debug?]} @system
                                    _ (when debug? (println "RENDER - " hook))
                                    ;dereferences and initializes
                                    state-map (resolve-reactions reactions-map initial-values)

                                    local-state (core-get state-map hook)
                                    foreign-states (dissoc state-map hook)

                                    ;options

                                    _ (swap! component-states assoc id {:handle         handle
                                                                        :local-state    local-state
                                                                        :foreign-states foreign-states})
                                    render (:render r)]

                                (if-not render
                                  [:div (str "No render: " hook ctx)]

                                  (let [rendered (render handle local-state foreign-states opts) ;instead of reagent calling render function - we do it
                                        [elm elm-opts & remaining] rendered

                                        ;insert dom-id
                                        result (into (if (map? elm-opts)
                                                       [elm (assoc elm-opts :id id)]
                                                       [elm {:id id} elm-opts]) remaining)]



                                    (if debug?
                                      (insert-debug-info result (core-get @component-states id))
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
   (core-get @component-states id)))


(defn mk-id [handle sub-id] (str (:id handle) sub-id))

(defn update [state {:keys [id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-handle-data id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! state/state #(apply update % handle args)))

(defn dispatch [{:keys [hook id]} dispatch-f & args]
  (let [h-data (get-handle-data id)
        f (-> @state/state
                       (get-in [:hooks hook])
                       (core-get dispatch-f))]
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