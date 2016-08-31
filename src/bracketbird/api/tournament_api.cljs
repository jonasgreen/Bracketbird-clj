(ns bracketbird.tournament-api
  "Defines the api to the tournament in form of events.
  Takes one or more tournament events and executes them blindly on the tournament, ie. updates the tournament."
  (:require [bracketbird.model.tournament :as tournament]
            [bracketbird.context :as ctx]
            [bracketbird.util.uuid :as uid]
            [bracketbird.application-state :as app-state]))


(defn event [event-type]
  {:event-id   (uid/squuid)
   :event-type event-type})

(defn- tournament-event [event-type tournament-id]
  (-> (event event-type)
      (assoc :tournament-id tournament-id)))

(defn- team-event [event-type team-id]
  (-> (event event-type)
      (assoc :team-id team-id)))


;-------------
; api-events
;-------------

(defn create-tournament-event [tournament-id]
  (tournament-event [:tournament :create] tournament-id))

(defn add-team-event []
  (team-event [:team :add] (uid/squuid)))

(defn delete-team-event [team-id]
  (team-event [:team :delete] team-id))

(defn update-team-name-event [team-id name]
  (-> (team-event [:team :name :update] team-id)
      (assoc :name name)))

(defn update-team-seeding-event [team-id seeding]
  (-> (team-event [:team :seeding :update] team-id)
      (assoc :seeding seeding)))




(defmulti execute (fn [event t] (:event-type event)))

(defn update-state [t] t)

(defn execute-api-event [t-ctx event]
  (->> (ctx/data t-ctx)
       (execute event)
       (tournament/add-history event)
       (update-state)
       (ctx/swap-data! t-ctx)))


;------------------------
; executing api events
;------------------------

;create tournament
(defmethod execute [:tournament :create] [e t]
  (println "execute" t)
  (tournament/mk (:tournament-id e)))

;add
(defmethod execute [:team :add] [e t]
  (tournament/add-team t (:team-id e)))

;update
(defmethod execute [:team :name :update] [e t]
  (tournament/update-team-name t (:team-id e) (:name e)))

(defmethod execute [:team :seeding :update] [e t]
  (tournament/update-team-seeding t (:team-id e) (:seeding e)))

;delete
(defmethod execute [:team :delete] [e t]
  (tournament/delete-team t (:team-id e)))



