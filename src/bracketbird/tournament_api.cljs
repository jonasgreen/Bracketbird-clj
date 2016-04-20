(ns bracketbird.tournament-api
  (:require [bracketbird.model.tournament-event :as t-event]
            [bracketbird.model.tournament :as tournament]
            [bracketbird.model.team :as team]))



(defmulti execute (fn [tournament event] (:type event)))

(defmethod execute [:create-team] [tournament e])
(defmethod execute [:create-team :possible?] [tournament e])