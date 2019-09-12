(ns bracketbird.ui
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [reagent.core :as r]))

(declare gui hook-path ui-update handle)

(defonce component-states (atom {}))

(defn hook? [h]
  (and (keyword? h) (= "hooks" (namespace h))))

(defn init [ui-hook]
  (get-in @state/state [:hooks ui-hook :values]))


(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [hv (get hooks h)
        p (if (ut/ui-hook? h) (:path hv) hv)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn hook-path [h ctx]
  (let [path (resolve-path (:hooks @state/state) h)]

    (->> (if (ut/ui-hook? h) (conj path :values) path)
         ;replace id's
         (map (fn [p] (get ctx p p)))
         (vec))))


(defn resolve-args [{:keys [ctx hook path]} args]
  (if (hook? (first args))
    {:r-hook (first args)
     :r-path (hook-path (first args) ctx)
     :r-f    (second args)
     :r-args (nnext args)}
    {:r-hook hook
     :r-path path
     :r-f    (first args)
     :r-args (next args)}))


(defn will-update [opts args]
  (let [{:keys [r-path r-f r-args r-hook] :as rlv} (resolve-args opts args)]
    (fn [state]
      (let [fn-ensure-init (fn [m] (let [m (if m m (init r-hook))]
                                     (apply r-f m r-args)))]
        (update-in state r-path fn-ensure-init)))))


(defn ui-dispatch [{:keys [id hook] :as opts} args]
  (let [{:keys [local-state foreign-states]} (get @component-states id)
        dispatch-f (-> @state/state
                       (get-in [:hooks hook :fns])
                       (get (first args)))]
    (when-not dispatch-f (throw (js/Error. (str "Dispatch function " (first args) " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply dispatch-f local-state foreign-states (partial handle opts) (next args))))


(defn ui-update [opts args]
  (if (vector? (first args))
    (fn [state] (reduce (fn [s v] ((will-update opts v) s)) state args))
    (swap! state/state (will-update opts args))))



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

(defn ui-build
  ([hook] (ui-build hook {}))
  ([hook ctx] (ui-build hook ctx {}))
  ([hook ctx next-ctx] (gui hook (merge ctx next-ctx))))


(defn handle [{:keys [id ctx path hook] :as m} & opts]
  (let [one (first opts)
        two (first (next opts))
        third (first (nnext opts))]

    #_(println "one two three" one two third)

    (condp = one
      :build (ui-build two ctx third)
      :update (ui-update m (rest opts))
      :dispatch (ui-dispatch m (rest opts))
      :ctx ctx
      :path path
      :hook hook
      :id id
      :else (throw (js/Error. (str "Handle " (first opts) " not supported"))))))


(defn gui [hook ctx]
  {:pre [(keyword? hook) (map? ctx)]}
  (println "NEW GUI - " hook)
  (let [system (state/subscribe [:system] ctx)

        r (get-in @state/state [:hooks hook])
        all-hooks (into [hook] (:reactions r))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        id (hash (get all-paths hook))
        path (get all-paths hook)

        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:hooks hook :values] {})
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
        f (partial handle options)]

    (fn [_ _]
      (println "RENDER - " hook)

      (let [{:keys [debug?]} @system

            ;dereferences and initializes
            state-map (resolve-reactions reactions-map initial-values)

            local-state (get state-map hook)
            foreign-states (dissoc state-map hook)

            ;options

            _ (swap! component-states assoc id {:options        options
                                                :local-state    local-state
                                                :foreign-states foreign-states})
            render (:render r)]

        (if-not render
          [:div (str "No render: " hook ctx)]

          (let [rendered (render local-state foreign-states f) ;instead of reagent calling render function - we do it
                [elm elm-opts & remaining] rendered

                ;insert dom-id
                result (into (if (map? elm-opts)
                               [elm (assoc elm-opts :id id)]
                               [elm {:id id} elm-opts]) remaining)]



            (if debug?
              (insert-debug-info result (get @component-states id))
              result)))))))