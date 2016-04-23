(ns bracketbird.model.tournament
  (:require [bracketbird.util :as util]
            [bracketbird.model.team :as team]))

(defn create [entity-id]
  {:entity-id entity-id
   :teams     []
   :stages    []
   :final-ranking []})


(defn reset [t])

(defn layout-matches [t])

(defn rebuild [t]
  (-> (reset t)
      (layout-matches)))

;-------
; teams
;-------

(defn teams [t]
  (:teams t))

(defn team [t team-id]
  (util/entity (teams t) team-id))

(defn add-team [t team-id]
  (-> (update :teams conj (team/create team-id))
      (rebuild)))

(defn- update-team [t team-id k value]
  (update-in t [:teams (util/index-of-entity (teams t) team-id)] assoc k value))

(defn update-team-name [t team-id name]
  (update-team t team-id :name name))

(defn update-team-seeding [t team-id seeding]
  (-> (update-team t team-id :seeding seeding)
      (rebuild)))

(defn delete-team [t team-id]
  (-> (assoc t :teams (util/remove-entity (teams t) team-id))
      (rebuild)))