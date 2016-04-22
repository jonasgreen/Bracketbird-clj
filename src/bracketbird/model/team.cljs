(ns bracketbird.model.team
  (:require [bracketbird.model.tournament-event :as t-event-m]
            [bracketbird.model-util :as util]))



(defn team [e]
  (-> {}
      (merge (select-keys e [:model-id :team-name]))))