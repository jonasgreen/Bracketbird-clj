(ns bracketbird.tournament-api
  "Defines the api to the tournament in form of events.
  Takes one or more tournament events and executes them blindly on the tournament, ie. updates the tournament."
  (:require [bracketbird.model.tournament :as tournament]
            [bracketbird.contexts.context :as ctx]
            [bracketbird.model.uuid :as uuid]))



(defn event [event-type]
  {:event-id         (uuid/squuid)
   :event-type event-type})

(defn- tournament-event [event-type tournament-id]
  (-> (event event-type)
      (assoc :tournament-id tournament-id)))

(defn- team-event [event-type team-id]
  (-> (event event-type)
      (assoc :team-id team-id)))


;***********************
; Events
;***********************

(defn create-tournament-event []
  (tournament-event [:tournament :create] (uuid/squuid)))

;-------------
; team-events
;-------------

(defn add-team-event []
  (team-event [:team :add] (uuid/squuid)))

(defn delete-team-event [team-id]
  (team-event [:team :delete] team-id))

(defn update-team-name-event [team-id name]
  (-> (team-event [:team :name :update] team-id)
      (assoc :name name)))

(defn update-team-seeding-event [team-id seeding]
  (-> (team-event [:team :seeding :update] team-id)
      (assoc :seeding seeding)))




(defmulti execute (fn [t event] (:event-type event)))

(defn update-state [t])

(defn- execute-api-event [t-ctx event]
  (->> (ctx/get-data t-ctx)
       (execute event)
       (update-state)
       (ctx/swap-data! t-ctx)))



(defmethod execute [:tournament :create] [e]
  (tournament/create (:entity-id e)))


;------------------------
; executing teams events
;------------------------

;add
(defmethod execute [:team :add] [t e]
  (tournament/add-team t (:entity-id e)))

;update
(defmethod execute [:team :name :update] [t e]
  (tournament/update-team-name t (:entity-id e) (:name e)))

(defmethod execute [:team :seeding :update] [t e]
  (tournament/update-team-seeding t (:entity-id e) (:seeding e)))

;delete
(defmethod execute [:team :delete] [t e]
  (tournament/delete-team t (:entity-id e)))




