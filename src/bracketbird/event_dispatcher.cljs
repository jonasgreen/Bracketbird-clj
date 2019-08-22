(ns bracketbird.event-dispatcher
  (:require [bracketbird.state :as state]))


(defn- execute-events [state events {:keys [execute-event aggregate-coeffect events-path aggregate-path]}]
  (let [aggregate (-> (reduce execute-event (get-in aggregate-path state {}) events)
                      (aggregate-coeffect))]
    (-> state
        (assoc-in aggregate-path aggregate)
        (update-in events-path (fnil into []) (vec events)))))


(defn dispatch-client-event [{:keys [event state-coeffect] :as m}]
  (let [new-state (-> @state/state
                      (execute-events [event] m)
                      (state-coeffect))]

    (reset! state/state new-state)))


(defn dispatch-server-event [])




