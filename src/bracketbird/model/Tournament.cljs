(ns bracketbird.model.tournament
  (:require [bracketbird.model-util :as m-util]
            [bracketbird.model.team :as team]))

(defn create [entity-id]
  {:entity-id entity-id
   :teams     []
   :stages    []})

(defn teams [t]
  (:teams t))

(defn team [t team-id]
  (m-util/get-entity (teams t) team-id))

(defn add-team [t team-id]
  (update t :teams conj (team/create team-id)))

(defn update-team-name [t team-id name])
