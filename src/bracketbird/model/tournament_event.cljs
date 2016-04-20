(ns bracketbird.model.tournament-event
  (:require [bracketbird.model.uuid :as uuid]))



(defn dna [type t-id model-id]
  {:id            (uuid/squuid)
   :tournament-id t-id
   :model-id      model-id
   :type          type})


(deftype create-team [name])
(deftype update-team [name])

(defn model-id [t-event]
  (:model-id t-event))