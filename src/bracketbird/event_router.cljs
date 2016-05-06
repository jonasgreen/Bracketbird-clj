(ns bracketbird.event-router
  (:require [reagent.ratom :as ratom]
            [bracketbird.model.uuid :as uuid]))

(defprotocol IEvent-subscriber
  (-execute [this events])
  (-re-execute [this events])
  (-error [this event]))

(defprotocol IRouter
  (-dispatch [this content bumb-level? description])
  (-add-subscriber [this subscriber])
  (-remove-subscriber [this id]))




(defn create-packet [state-atom router-id event bump-level? description]
  {:id          (uuid/squuid)
   :router-id router-id
   :count       (+ (count (:packets @state-atom)) (count (:out @state-atom)))
   :level-count 1
   :content     0})

(defn- out [state router-id event bumb-level?]
  ;send to all subscribers
  (if (empty? (:pending-events @state))
    ();send
    ();conj
    ))

(defn create-router [id]
  (let [state (ratom/atom {:packets   []
                           :pending-events []
                           :receivers {}})]
    (reify IRouter
      (-dispatch [router event bumb-level?]
        (out state id event bumb-level?))

      (-add-subscriber [this subscriber]
        (let [receivers (:receivers @state)
              id (inc (count receivers))]
          (swap! state update-in [:receivers] assoc id subscriber)
          id))

      (-remove-subscriber [router id]
        (swap! state update-in [:receivers] dissoc id)))))



