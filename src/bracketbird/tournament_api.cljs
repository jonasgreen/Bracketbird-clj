(ns bracketbird.tournament-api
  (:require [bracketbird.model.tournament :as tournament]
            [bracketbird.contexts.context :as ctx]
            [bracketbird.model.uuid :as uuid]))


(defn event [event-type]
  {:id         (uuid/squuid)
   :event-type event-type})

;-------------
; team-events
;-------------

(defn- team-event [event-type team-id]
  (-> (event event-type)
      (assoc :team-id team-id)))

(defn add-team-event [team-id]
  (team-event :add-team team-id))

(defn delete-team-event [team-id]
  (team-event :delete-team team-id))

(defn update-team-name-event [team-id name]
  (-> (team-event :update-team-name team-id)
      (assoc :name name)))

(defn update-team-seeding-event [team-id seeding]
  (-> (team-event :update-team-name team-id)
      (assoc :seeding seeding)))




(defmulti execute (fn [t event] (:event-type event)))

(defn update-state [t])

(defn- execute-api-event [t-ctx event]
  (->> (ctx/get-data t-ctx)
       (execute event)
       (update-state)
       (ctx/swap-data! t-ctx)))


;------------------------
; executing teams events
;------------------------

;create
(defmethod execute [:add-team] [t e]
  (tournament/add-team t (:entity-id e)))

;update
(defmethod execute [:update-team-name] [t e]
  (tournament/update-team-name t (:entity-id e) (:name e)))

(defmethod execute [:update-team-seeding] [t e]
  (tournament/update-team-seeding t (:entity-id e) (:seeding e)))

;delete
(defmethod execute [:delete-team] [t e]
  (tournament/delete-team t (:entity-id e)))




