(ns bracketbird.event-router
  (:require [reagent.ratom :as ratom]
            [bracketbird.tournament-api :as t-api]
            [bracketbird.util :as ut]))

(defn pending-packet [router-id event]
  {:id          (ut/squuid)
   :router-id   router-id
   :count       nil                                         ;(+ (count (:packets @state-atom)) (count (:out @state-atom)))
   :level-count nil                                         ;1
   :event       event})

(defn- send [state event bumb-level?])

(defn dispatch [ctx api-event]
  (println "dispatchevent" api-event)
  (println "dispatchctx" ctx)

  (t-api/execute-api-event ctx api-event))

(defn create-router [id subscriber]
  (let [state (ratom/atom {:router-id       id
                           :subscriber      subscriber
                           :history-packets []
                           :pending-packets []})]
    (fn [event bump-level?]
      (if (empty? (:pending-events @state))
        (send state event bump-level?)
        (swap! state update-in [:pending-packets] conj (pending-packet id event))))))



