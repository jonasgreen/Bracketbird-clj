(ns bracketbird.model.team
  (:require [bracketbird.model.entity :as e]))

(defn mk-kw [kw-name]
  (keyword "team" kw-name))

(def team-name :team-name)                                  ;(mk-kw 'name))
(def team-id :team-id)                                      ;(mk-kw 'id))


(defrecord Team [team-id team-name]
  e/IEntity
  (-id [this] team-id))

(defn mk-team [t-id t-name]
  (->Team t-id t-name))