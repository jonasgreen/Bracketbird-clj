(ns bracketbird.event-dispatcher
  (:require [bracketbird.state :as state]))


(defn- execute-events [state events event-executors {:keys [aggregate-coeffect events-path aggregate-path]}]
  (let [agg-before (get-in state aggregate-path {})
        _ (println "agg before" agg-before)
        agg-after (->> (map vector event-executors events)
                       (reduce (fn [agg [f e]] (f agg e)) agg-before)
                       aggregate-coeffect)

        _ (println "agg after" agg-after)
        ]
    (-> state
        (assoc-in aggregate-path agg-after)
        (update-in events-path (fnil into []) (vec events)))))


(defn dispatch-client-event [{:keys [event execute-event state-coeffect] :as m}]
  (let [new-state (-> @state/state
                      (execute-events [event] [execute-event] m)
                      (state-coeffect))]

    (reset! state/state new-state)))


(defn dispatch-server-event [])




