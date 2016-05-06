(ns bracketbird.model.tournament-entity)

(def not-ready 0)
(def ready 1)
(def in-progress 2)
(def rank-equality 3)
(def finished 4)

(defprotocol IEntity
  (-id [this])
  (-state [this]))


(defn started?
  [entity]
  (> (-state entity) in-progress))

