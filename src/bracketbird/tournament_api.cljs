(ns bracketbird.tournament-api
  (:require [bracketbird.model.tournament :as tournament]
            [bracketbird.contexts.context :as ctx]))

(defmulti execute (fn [t event] (:event-type event)))

(defn update-state [t])

(defn- execute-api-event [t-ctx event]
  (->> (ctx/get-data t-ctx)
       (execute event)
       (update-state)
       (ctx/swap-data! t-ctx)))

(defn api-event [event-type ctx-id model-id]
  {:event-type event-type
   :ctx-id     ctx-id
   :model-id   model-id})

;-------
; teams
;-------

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




