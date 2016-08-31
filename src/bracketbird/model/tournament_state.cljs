(ns bracketbird.model.tournament-state)

(def not-ready 0)
(def ready 1)
(def in-progress 2)
(def rank-equality 3)
(def finished 4)

(defprotocol IState
  (-id [this])
  (-state [this]))


(defn started?
  [entity]
  false
  #_(> (-state entity) in-progress))

