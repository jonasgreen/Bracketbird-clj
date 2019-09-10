(ns bracketbird.ui
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [reagent.core :as r]))

(declare gui hook-path ui-update)


(defn hook? [h]
  (and (keyword? h) (= "hooks" (namespace h))))

(defn init [ui-hook]
  (get-in @state/state [:hooks ui-hook :values]))


(defn resolve-f [r-hook opts f]
  (cond
    (fn? f) f
    (seq? f) (first f)
    (keyword? f) (let [dispatch-f (f (get-in @state/state [:hooks r-hook :fns]))]
                   (when-not dispatch-f (throw (js/Error. (str "Dispatch function " f " is not defined in hook " r-hook))))
                   ;make ui-update available to dispatch functions
                   (r/partial dispatch-f (assoc opts :ui-update (r/partial ui-update opts))))

    :else (throw (js/Error. (str "Unable to resolve function " f)))))

(defn resolve-args [{:keys [ui-ctx ui-hook ui-path] :as opts} f & args]
  (if (hook? f)
    {:r-hook f
     :r-path (hook-path f ui-ctx)
     :r-f    (resolve-f f opts (first args))
     :r-args (rest args)}
    {:r-hook ui-hook
     :r-path ui-path
     :r-f    (resolve-f ui-hook opts f)
     :r-args args}))


(defn will-update [opts f & args]
  (let [{:keys [r-path r-f r-args r-hook]} (apply resolve-args opts f args)]
    (fn [state]
      (let [fn-ensure-init (fn [m] (let [m (if m m (init r-hook))]
                                     (apply r-f m r-args)))]
        (update-in state r-path fn-ensure-init)))))

(defn ui-update-swap! [opts f & args]
  (swap! state/state (apply will-update opts f args)))

(defn ui-dispatch [{:keys [ui-hook] :as opts}]
  (fn [f & args]
    {:pre [(keyword? f)]}

    (let [dispatch-f (f (get-in @state/state [:hooks ui-hook :fns]))]
      (when-not dispatch-f (throw (js/Error. (str "Dispatch function " f " is not defined in hook " ui-hook))))
      ;make ui-update available to dispatch functions
      (apply dispatch-f (dissoc opts :ui-build :ui-dispatch) args))))

(defn ui-update [opts]
  (fn [f & args]
    (if (vector? f)
      (fn [state]
        (reduce (fn [s v] ((apply will-update opts (first v) (rest v)) s)) state (into [f] args)))
      (apply ui-update-swap! opts f args))))

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
        (dissoc hook)
        )))


(defn ui-build
  ([hook] [ui-build {} hook])
  ([ctx hook & next-ctx]
   (gui (merge ctx (first next-ctx)) hook))

  )

(defn gui [ctx hook]
  {:pre [(keyword? hook) (map? ctx)]}
  #_(println "GUI" hook)
  (let [system (state/subscribe [:system] ctx)
        r (get-in @state/state [:hooks hook])

        all-hooks (into [hook] (:reactions r))
        ui-hooks (filter ut/ui-hook? all-hooks)



        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx)))
                          {}
                          all-hooks)


        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:hooks hook :values])
                                              {})))
                               {}
                               all-hooks)

        static-decorations (reduce (fn [m h] (let [path (h all-paths)]
                                               (assoc m h (merge {:ui-dom-id (hash path)
                                                                  :ui-ctx    ctx
                                                                  :ui-hook   h
                                                                  :ui-path   path
                                                                  :ui-build  (r/partial ui-build ctx)}))))
                                   {}
                                   ui-hooks)

        reactions (reduce (fn [m h] (assoc m h (state/subscribe (h all-paths)))) {} all-hooks)]


    (fn [_ _]
      (println "RENDER" hook "CTX" ctx)
      (let [{:keys [debug?]} @system
            options-r (resolve-options hook reactions initial-values static-decorations)
            options-r1 (assoc options-r :ui-update (r/partial (ui-update options-r)))
            options (assoc options-r1 :ui-dispatch (r/partial (ui-dispatch options-r1)))

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
                (println "static-decorations" static-decorations)
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