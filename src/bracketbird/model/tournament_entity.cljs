(ns bracketbird.model.tournament-entity)

(def state-not-ready 0)
(def state-ready 1)
(def state-in-progress 2)
(def state-rank-equality 3)
(def state-finished 4)

(defprotocol ITournament-entity
  (-state [this]))

