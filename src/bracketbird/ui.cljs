(ns bracketbird.ui
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]))



(defn resolve-initial-ui-values [ctx ui-hook values]
  (assoc values :dom-id (hash (state/hook-path ui-hook ctx))))

(defn modify-element-options [elem-opts options debug?]
  (let [m (-> elem-opts (assoc :id (:dom-id (:values options))))]
    (if debug?
      (update m :style assoc :border "1px solid black")
      m)))

(defn modify-reagent-result [result hook debug?]
  (if debug?
    (let [[start end] (split-at 2 result)
          debug-element [:div {:style {:font-size 10
                                       :position :relative
                                       :background :yellow
                                       :align-self :flex-start
                                       :display :table-cell}}
                         hook]]
      (vec (concat start [debug-element] end)))
    result))

(defn resolve-options [hook initial-ui-values reactions]
  (let [options (reduce-kv (fn [m k r]
                             (let [v (deref r)]
                               (assoc m k (if (and (ut/ui-hook? k)
                                                   (= 1 (count v))) ;empty ui-values is a map with :_ui-path in it
                                            (merge (get initial-ui-values k) v)
                                            v))))
                           {} reactions)]
    (-> options
        (assoc :values (hook options))
        (dissoc hook))))

(defn gui [hook ctx]
  (let [system (state/hook :hooks/system ctx)
        r (get-in @state/state [:renders hook])

        ui-hooks (filter ut/ui-hook? (into [hook] (:reactions r)))
        data-hooks (filter (comp not ut/ui-hook?) (:reactions r))

        ;should not be reaction dependent
        initial-ui-values (reduce (fn [m h]
                                    (let [iv (get-in @state/state [:renders hook :values] {})]
                                      (assoc m h (resolve-initial-ui-values ctx h iv)))) {} ui-hooks)

        reactions (reduce (fn [m h] (assoc m h (state/hook h ctx))) {} (into ui-hooks data-hooks))]

    (fn [hook ctx]
      (println "RENDER" hook)
      (let [{:keys [debug?]} @system
            options (resolve-options hook initial-ui-values reactions)
            render (:render r)]

        (if-not render
          [:div (str "No render: " hook ctx)]

          (let [rendered (render ctx options)               ;instead of reagent calling render function - we do it
                [elm elm-opts & remaining] rendered
                modified-elm-opts (if (map? elm-opts)
                                    (into [elm (modify-element-options elm-opts options debug?)] remaining)
                                    (into [elm (modify-element-options {} options debug?) elm-opts] remaining))

                result (modify-reagent-result modified-elm-opts hook debug?)]

            (when debug?
              (println "RENDERED" rendered)
              (println "ELM" elm)
              (println "ELM-OPTS" elm-opts)
              (println "REMAINING" remaining)
              (println "MODIFIED-ELM-OPTS" modified-elm-opts)
              (println "RESULT" result))

            result))))))