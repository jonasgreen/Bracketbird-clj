(ns bracketbird.ui
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]))

(defn init [ui-hook]
  (get-in @state/state [:renders ui-hook :values]))


(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [p (get hooks h)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn hook-path [h ctx]
  (->> (resolve-path (:hooks @state/state) h)
       ;replace id's
       (map (fn [p] (get ctx p p)))
       (vec)))


(defn insert-debug-info [result options]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [e]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:hook options)))
                                             (println "CTX\n" (b-ut/pp-str (:ctx options)))
                                             (println "OPTIONS\n" (b-ut/pp-str options)))
                                 }
                        (:hook options)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))


(defn modify-element-options [elem-opts options]
  (-> elem-opts (assoc :id (:dom-id (:values options)))))

(defn resolve-options [hook reactions initial-values decorations]
  (let [options (reduce-kv (fn [m h r]
                             (let [v (deref r)
                                   v1 (if v v (hook initial-values))
                                   d (h decorations)
                                   value (if d (merge v1 d) v1)]
                               (assoc m h value)))
                           {} reactions)]

    (-> options
        (merge (hook options))
        (dissoc hook))))

(defn gui [hook ctx]
  (let [system (state/subscribe [:system] ctx)
        r (get-in @state/state [:renders hook])

        all-hooks (into [hook] (:reactions r))
        ui-hooks (filter ut/ui-hook? all-hooks)

        all-paths (reduce (fn [m h] (let [path (hook-path h ctx)]
                                      (assoc m h (if (ut/ui-hook? h) (conj path :values) path))))
                          {}
                          all-hooks)

        decorations (reduce (fn [m h] (let [path (h all-paths)]
                                        (assoc m h (merge {:ctx    ctx
                                                           :hook   h
                                                           :path   path
                                                           :dom-id (hash path)}
                                                          (:fns (get-in @state/state [:renders h]))))))
                            {}
                            ui-hooks)



        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:renders hook :values])
                                              {})))
                               {}
                               all-hooks)

        reactions (reduce (fn [m h] (assoc m h (state/subscribe (h all-paths)))) {} all-hooks)]


    (fn [hook ctx]
      (println "RENDER" hook)
      (let [{:keys [debug?]} @system
            options (resolve-options hook reactions initial-values decorations)
            render (:render r)]

        (if-not render
          [:div (str "No render: " hook ctx)]

          (let [rendered (render options)                   ;instead of reagent calling render function - we do it
                [elm elm-opts & remaining] rendered
                result (if (map? elm-opts)
                         (into [elm (modify-element-options elm-opts options)] remaining)
                         (into [elm (modify-element-options {} options) elm-opts] remaining))]
            (if debug?
              (do
                (println "\n----- " hook)
                (println "all hooks" all-hooks)
                (println "all paths" all-paths)
                (println "decorations" decorations)
                (println "initial values" initial-values)
                (println "options" options)



                (println "RENDERED" rendered)
                (println "ELM" elm)
                (println "ELM-OPTS" elm-opts)
                (println "REMAINING" remaining)
                (println "MODIFIED-ELM-OPTS" result)
                (println "RESULT" result)
                (insert-debug-info result options))
              result)))))))


(defn put! [ui-values k v & kvs]
  {:pre [(map? ui-values)]}
  (let [path (:path ui-values)
        hook (:hook ui-values)]

    (swap! state/state update-in path (fn [m] (apply assoc (if m m (init hook)) k v kvs)))))

;deprecation
(defn hook
  ([h ctx]
   (hook h ctx nil))
  ([h ctx not-found]
   (let [path (hook-path h ctx)]
     (state/subscribe path not-found))))