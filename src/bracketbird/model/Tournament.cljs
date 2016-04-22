(ns bracketbird.model.tournament
  (:require [bracketbird.model-util :as m-util]))

(defn create []
  {:teams []
   :stages []})


(defn teams [t]
  (:teams t))

(defn team [t id]
  (m-util/get-model (teams t) id))

(defn add-team [t team-id]
  (update t :teams conj {:id}))

(defn update-team-name [team-id name]

  )
