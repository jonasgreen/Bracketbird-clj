(ns bracketbird.api.tournament-api_old
  "Defines the api to the tournament in form of events.
  Takes one or more tournament events and executes them blindly on the tournament, ie. updates the tournament."
  (:require [bracketbird.model.tournament :as tournament]
            [bracketbird.old-context :as ctx]
            [bracketbird.util :as ut]))


;-------
; utils
;-------

(defn event [event-type]
  {:event-id   (ut/squuid)
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

(defn add-team-event [name]
  (-> (team-event [:team :add] (ut/squuid))
      (assoc :name name)))

(defn insert-team-event [name index]
  (-> (team-event [:team :insert] (ut/squuid))
      (assoc :name name :index index)))


(defn delete-team-event [team-id]
  (team-event [:team :delete] team-id))

(defn update-team-name-event [team-id name]
  (-> (team-event [:team :name :update] team-id)
      (assoc :name name)))

(defn update-team-seeding-event [team-id seeding]
  (-> (team-event [:team :seeding :update] team-id)
      (assoc :seeding seeding)))

;----------------------
; executing api events
;----------------------

(defmulti execute (fn [event t] (:event-type event)))

(defn update-state [t] t)

(defn execute-api-event [t-ctx event]
  (->> (ctx/data t-ctx)
       (execute event)
       (tournament/add-history event)
       (update-state)
       (ctx/swap-data! t-ctx)))

;-----------------------------------
; api events execution counterparts
;-----------------------------------

;create tournament
(defmethod execute [:tournament :create] [e t]
  (tournament/mk (:tournament-id e)))

;add
(defmethod execute [:team :add] [{:keys [team-id name]} t]
  #_(tournament/add-team t team-id name))

(defmethod execute [:team :insert] [{:keys [team-id name index]} t]
  (tournament/insert-team t team-id name index))


;update
(defmethod execute [:team :name :update] [e t]
  (tournament/update-team-name t (:team-id e) (:name e)))

(defmethod execute [:team :seeding :update] [e t]
  (tournament/update-team-seeding t (:team-id e) (:seeding e)))

;delete
(defmethod execute [:team :delete] [e t]
  (tournament/delete-team t (:team-id e)))




