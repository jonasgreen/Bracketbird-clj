(ns bracketbird.event-dispatcher
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [bracketbird.state :as state]
            [bracketbird.system :as system]
            [cljs.core.async :refer [put! chan <!]]
            [reagent.core :as r]))


(defn test-server []
  (let [events (atom [])
        handle-incoming-event (fn [event])]

    handle-incoming-event))

(defn http-server []);todo


(defonce channel-out
         (let [out (chan)
               handler (if (system/test?) test-server http-server)]
           (go (while true
                 (let [packet (<! out)]
                   ;(.log js/console (str "handle-outgoing: " packet))
                   (handler packet))))
           out))

(defonce channel-in
         (let [in (chan)]
           (go (while true
                 (let [packet (<! in)]
                   (.log js/console (str "handle-incoming: " packet)))))
           in))


(defn- execute-events [state events event-executors {:keys [aggregate-coeffect events-path aggregate-path]}]
  (let [agg-before (get-in state aggregate-path {})
        agg-after (->> (map vector event-executors events)
                       (reduce (fn [agg [f e]] (f agg e)) agg-before)
                       aggregate-coeffect)]
    (-> state
        (assoc-in aggregate-path agg-after)
        (update-in events-path (fnil into []) (vec events)))))



(defn dispatch-client-event [{:keys [event execute-event state-coeffect post-render] :as m}]
  (let [new-state (-> @state/state
                      (execute-events [event] [execute-event] m)
                      state-coeffect)]
    (r/after-render #(post-render event))
    (reset! state/state new-state)
    (put! channel-out {:request :persist :event event})))



