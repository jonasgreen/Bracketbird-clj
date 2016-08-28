(ns bracketbird.tournament-controller
  (:require [bracketbird.contexts.context :as ctx]
            [bracketbird.model.tournament-state :as state]
            [bracketbird.event-router :as router]
            [bracketbird.tournament-api :as api]))



(defn create-tournament []

  #_(router/dispatch (api/create-tournament-event ))
  )


(defn add-team [t-ctx]
  (if (-> t-ctx ctx/get-data state/started?)
    (println "warning - tournament already started")
    (router/dispatch t-ctx (api/add-team-event))))

(defn update-team-name [t-ctx team-id name]
  (router/dispatch t-ctx (api/update-team-name-event team-id name)))

(defn delete-team [t-ctx team-id]
  (if (-> t-ctx ctx/get-data state/started?)
    (println "warning - tournament already started")
    (router/dispatch t-ctx (api/delete-team-event team-id))))
