(ns bracketbird.tournament-controller
  (:require [bracketbird.contexts.context :as ctx]
            [bracketbird.model.tournament-entity :as entity]
            [bracketbird.event-router :as router]
            [bracketbird.tournament-api :as api]))



(defn create-tournament [])


(defn add-team [t-ctx]
  (let [t (ctx/get-data t-ctx)
        started? (entity/started? t)]
    (if started?
      (println "warning - tournament already started")
      (router/dispatch t-ctx (api/add-team-event) started?))))


(defn update-team-name [t-ctx name])