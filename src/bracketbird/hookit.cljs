(ns bracketbird.hookit
  (:refer-clojure :exclude [update get])
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [goog.dom :as dom]
            [reagent.core :as r]))

(declare gui hook-path ui-update mk-hook-handle local-state get-handle foreign-handle dynamic-api ctx)

(defonce component-states (atom {}))

(def core-get cljs.core/get)

(defn hook? [h]
  (and (keyword? h) (= "hooks" (namespace h))))

(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [hv (core-get hooks h)
        p (if (ut/ui-hook? h) (:path hv) hv)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn hook-path [h ctx]
  (let [path (resolve-path (:hooks @state/state) h)]

    (->> (if (ut/ui-hook? h) (conj path :_local-state) path)
         ;replace id's
         (map (fn [p] (core-get ctx p p)))
         (vec))))

(defn insert-debug-info [result {:keys [options local-state foreign-states]}]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [e]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:hook options)))
                                             (println (str "RENDER RESULT\n" (b-ut/pp-str result)))
                                             (println "OPTIONS\n" (b-ut/pp-str options))
                                             (println "LOCAL-STATE\n" (b-ut/pp-str local-state))
                                             (println "FOREIGN-STATE-KEYS\n" (b-ut/pp-str (keys foreign-states))))}
                        (:hook options)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))

(defn resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (hook initial-values)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))

(defn- build [h hook next-ctx opts]
  [gui (merge (ctx h) next-ctx) hook opts])

(defn- mk-hook-handle [options]
  (partial dynamic-api options))

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

        options {:id            id
                 :ctx           ctx
                 :path          path
                 :hook          hook
                 :foreign-paths (-> all-paths
                                    (dissoc hook)
                                    vals
                                    vec)}

        f (mk-hook-handle options)]

    (r/create-class
      {:component-did-mount (fn [this]
                              (println "DID MOUNT - " hook)
                              (when-let [dm (:did-mount r)]
                                         (let [{:keys [local-state foreign-states]} (core-get @component-states id)]
                                           (dm local-state foreign-states f))))
       :reagent-render      (fn [_ _ opts]

                              ;(println "RENDER ARGS" opts)
                              (let [{:keys [debug?]} @system
                                    _ (when debug? (println "RENDER - " hook))
                                    ;dereferences and initializes
                                    state-map (resolve-reactions reactions-map initial-values)

                                    local-state (core-get state-map hook)
                                    foreign-states (dissoc state-map hook)

                                    ;options

                                    _ (swap! component-states assoc id {:options        options
                                                                        :local-state    local-state
                                                                        :foreign-states foreign-states})
                                    render (:render r)]

                                (if-not render
                                  [:div (str "No render: " hook ctx)]

                                  (let [rendered (render local-state foreign-states f opts) ;instead of reagent calling render function - we do it
                                        [elm elm-opts & remaining] rendered

                                        ;insert dom-id
                                        result (into (if (map? elm-opts)
                                                       [elm (assoc elm-opts :id id)]
                                                       [elm {:id id} elm-opts]) remaining)]



                                    (if debug?
                                      (insert-debug-info result (core-get @component-states id))
                                      result)))))})))


(defn get-id [ctx hook]
  (hash (hook-path hook ctx)))

(defn- get-handle-data
  ([ctx hook]
   (get-handle-data (get-id ctx hook)))
  ([id]
   (core-get @component-states id)))

(defn data-and-args [h args]
  (if (hook? (first args))
    {:h-data (get-handle-data (h :ctx) (first args))
     :args   (rest args)}
    {:h-data (get-handle-data (h :id))
     :args   args}))

(defn- update-impl [state {:keys [h-data args]}]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state h-data)) (rest args)))]
    (update-in state (-> h-data :options :path) upd)))


;;;; API


(defn id
  ([h] (h :id))
  ([h sub-id] (str (h :id) sub-id)))

(defn ctx [h] (h :ctx))

(defn ui-root
  ([hook] (build (mk-hook-handle {:ctx {}}) hook {} nil)))

(defn get [h hook]
  (if (ut/ui-hook? hook)
    (get-handle-data (h :ctx) hook)
    (get-in @state/state (hook-path hook (h :ctx)))))

(defn update [state h & args]
  (->> (data-and-args h args)
       (update-impl state)))

(defn put! [h & args]
  (swap! state/state #(apply update % h args)))

(defn dispatch [h args-org]
  (let [{:keys [h-data args]} (data-and-args h args-org)
        hook (-> h-data :options :hook)
        dispatch-f (-> @state/state
                       (get-in [:hooks hook])
                       (core-get (first args)))]
    (when-not dispatch-f (throw (js/Error. (str "Dispatch function " (first args) " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply dispatch-f (:local-state h-data) (:foreign-states h-data) (mk-hook-handle (:options h-data)) (next args))))



(defn get-element
  ([h] (-> h id dom/getElement))
  ([h sub-id] (-> h (id sub-id) dom/getElement)))

(defn get-handle [ctx hook]
  (-> (get-handle-data ctx hook)
      :options
      mk-hook-handle))

(defn foreign-handle [h foreign-hook]
  (get-handle (h :ctx) foreign-hook))

(defn- dynamic-api [{:keys [id ctx path hook] :as options} & args]
  (let [first-arg (first args)
        second-arg (second args)
        third-arg (first (nnext args))
        fourth-arg (second (nnext args))

        h-handle (mk-hook-handle options)]

    ;; update is treated special - it takes state as first arg
    (if (and (map? first-arg) (= second-arg :update))
      (apply update first-arg h-handle (nnext args))
      (condp = first-arg
        :build (build h-handle second-arg third-arg fourth-arg)
        ;:update (apply put! h-handle (rest args)) - when called without state it acts as a put!
        :put! (apply put! h-handle (rest args))
        :dispatch (dispatch h-handle (rest args))
        :get (get h-handle second-arg)
        :ctx ctx
        :path path
        :hook hook
        :id (if second-arg (str id second-arg) id)
        :options options
        :else (throw (js/Error. (str "Handle " (first args) " not supported")))))))
