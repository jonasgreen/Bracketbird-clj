(ns bracketbird.ui
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [reagent.core :as r]))

(declare gui hook-path ui-update)


(defonce handlers (atom {}))


(defn hook? [h]
  (and (keyword? h) (= "hooks" (namespace h))))

(defn init [ui-hook]
  (get-in @state/state [:hooks ui-hook :values]))

(defn resolve-args [{:keys [ctx hook path]} f & args]
  (if (hook? f)
    {:r-hook f
     :r-path (hook-path f ctx)
     :r-f    (first args)
     :r-args (rest args)}
    {:r-hook hook
     :r-path path
     :r-f    f
     :r-args args}))


(defn will-update [opts f & args]
  (let [{:keys [r-path r-f r-args r-hook]} (apply resolve-args opts f args)]
    (fn [state]
      (let [fn-ensure-init (fn [m] (let [m (if m m (init r-hook))]
                                     (apply r-f m r-args)))]
        (update-in state r-path fn-ensure-init)))))

(defn ui-update-swap! [opts f & args]
  (swap! state/state (apply will-update opts f args)))

(defn ui-dispatch [local-state foreign-states {:keys [hook] :as opts} f & args]
  {:pre [(keyword? f)]}
  (let [dispatch-f (f (get-in @state/state [:hooks hook :fns]))]
    (when-not dispatch-f (throw (js/Error. (str "Dispatch function " f " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply dispatch-f local-state foreign-states opts args)))


(defn ui-update [opts f & args]
  (if (vector? f)
    (fn [state]
      (reduce (fn [s v] ((apply will-update opts (first v) (rest v)) s)) state (into [f] args)))
    (apply ui-update-swap! opts f args)))

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


(defn modify-element-options [elem-opts {:keys [dom-id]}]
  (assoc elem-opts :id dom-id))


(defn resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (hook initial-values)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))

(defn ui-build
  [ctx hook & next-ctx]
  (gui (merge ctx (first next-ctx)) hook))

(defn root [hook]
  [ui-build {} hook])


(defn handle [id & opts])

(defn gui [ctx hook]
  {:pre [(keyword? hook) (map? ctx)]}
  (let [system (state/subscribe [:system] ctx)

        p (hook-path hook ctx)
        id (hash p)
        handler (r/partial handle id)

        (swap! handlers assoc id {:id id
                                  :path p
                                  :ctx ctx
                                  :hook hook})

        r (get-in @state/state [:hooks hook])
        all-hooks (into [hook] (:reactions r))

        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx)))
                          {}
                          all-hooks)

        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:hooks hook :values] {})
                                              {})))
                               {}
                               all-hooks)

        ; includes state and foreign state
        reactions-map (reduce (fn [m h] (assoc m h (state/subscribe (h all-paths)))) {} all-hooks)]

    (fn [_ _]
      (let [{:keys [debug?]} {:debug? false};@system


            ;dereferences and initializes
            state-map (resolve-reactions reactions-map initial-values)

            local-state (get state-map hook)
            foreign-states (dissoc state-map hook)

            ;options
            dom-id (hash (get all-paths hook))
            path (get all-paths hook)

            build-fn (r/partial ui-build ctx)
            update-fn (r/partial ui-update {:ctx ctx :hook hook :path path})

            dispatch-fn (r/partial ui-dispatch local-state foreign-states {:dom-id    dom-id
                                                                           :ctx       ctx
                                                                           :hook      hook
                                                                           :path      path
                                                                           :ui-update update-fn})

            options {:dom-id      dom-id
                     :ctx         ctx
                     :hook        hook
                     :path        path
                     :ui-build    build-fn
                     :ui-update   update-fn
                     :ui-dispatch dispatch-fn}

            render (:render r)]

        (if-not render
          [:div (str "No render: " hook ctx)]

          (let [rendered (render local-state foreign-states options) ;instead of reagent calling render function - we do it
                [elm elm-opts & remaining] rendered

                ;insert dom-id
                result (if (map? elm-opts)
                         (into [elm (modify-element-options elm-opts options)] remaining)
                         (into [elm (modify-element-options {} options) elm-opts] remaining))

                ]

            (let [{:keys [old-local-state old-result old-foreign-states old-options]} (get @state-atom hook)]
              (println "********" hook)
              (println "equal local state" (= old-local-state local-state))
              (println "equal foreign states" (= old-foreign-states foreign-states))
              (println "equal options" (= old-options options))
              (println "equal result" (= old-result result))


              (println "dom-id" (=(:dom-id old-options) (:dom-id options)))
              (println "ctx" (=(:ctx old-options) (:ctx options)))
              (println "hook" (=(:hook old-options) (:hook options)))
              (println "path" (=(:path old-options) (:path options)))
              (println "ui-build" (=(:ui-build old-options) (:ui-build options)))
              (println "ui-update" (=(:ui-update old-options) (:ui-update options)))
              (println "ui-dispatch" (=(:ui-dispatch old-options) (:ui-dispatch options)))
              (println "old-result" old-result)
              (println "result" result)


              ;(println "equal render-result " (= old-result result))
              (swap! state-atom update-in [hook] assoc :old-local-state local-state
                     :old-result result
                     :old-foreign-states foreign-states
                     :old-options options)
              )


            (if debug?
              (do
                (println "\n----- " hook)
                (println "all hooks" all-hooks)
                (println "all paths" all-paths)
                (println "initial values" initial-values)
                (println "local-state" local-state)
                (println "foreign-states" foreign-states)
                (println "options" options)

                (println "RESULT" result)
                (insert-debug-info result options))
              result)))))))