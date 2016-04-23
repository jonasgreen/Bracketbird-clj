(ns bracketbird.model.tournament
  (:require [bracketbird.util :as util]
            [bracketbird.model.team :as team]))

(defn create [entity-id]
  {:entity-id entity-id
   :teams     []
   :stages    []})

;-------
; teams
;-------

(defn teams [t]
  (:teams t))

(defn team [t team-id]
  (util/entity (teams t) team-id))

(defn add-team [t team-id]
  (update t :teams conj (team/create team-id)))

(defn update-team-name [t team-id name]
  (let [ie (util/index-and-entity (teams t) team-id)]
    (update-in t [:teams (first ie)] assoc :name name)))

(defn update-team-seeding [t team-id seeding])
(defn delete-team [t team-id])