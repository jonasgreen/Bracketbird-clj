(ns bracketbird.event-dispatcher
  (:require [bracketbird.state :as state]))


(defn- execute-events [state events events-spec {:keys [post-execution events-path aggregate-path]}]
  (let [execute-event (fn [agg {:keys [event-type] :as e}]
                        (-> events-spec
                            (get event-type)
                            :execute-event
                            (apply (seq [agg e]))))
        aggregate (-> (reduce execute-event (get-in aggregate-path state {}) events)
                      (post-execution))]
    (-> state
        (assoc-in aggregate-path aggregate)
        (update-in events-path (fnil into []) (vec events)))))


(defn dispatch-client-event [{:keys [event events-spec events-execution-spec post-update] :as m}]
  (let [new-state (-> @state/state
                      (execute-events [event] events-spec events-execution-spec)
                      (post-update))]

    (reset! state/state new-state)))


(defn dispatch-server-event [])




