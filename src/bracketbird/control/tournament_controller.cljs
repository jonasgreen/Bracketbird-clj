(ns bracketbird.tournament-controller
  (:require [bracketbird.context :as ctx]
            [bracketbird.model.tournament-state :as t-state]
            [bracketbird.event-router :as router]
            [bracketbird.tournament-api :as api]
            [bracketbird.application-state :as app-state]))


(defn mk-ctx [tournament-id]
  (ctx/mk :tournament tournament-id))

;--- utils ---

(defn count-tournaments []
  (count (get @app-state/state :tournament)))


;--- api ---

(defn create-tournament [t-ctx]
  (->> (api/create-tournament-event (:id t-ctx))
       (router/dispatch t-ctx)))


(defn add-team [t-ctx]
  (if (-> t-ctx ctx/data t-state/started?)
    (println "warning - tournament already started")
    (->> (api/add-team-event)
         (router/dispatch t-ctx))))

(defn update-team-name [t-ctx team-id name]
  (->> (api/update-team-name-event team-id name)
       (router/dispatch t-ctx)))

(defn delete-team [t-ctx team-id]
  (if (-> t-ctx ctx/data t-state/started?)
    (println "warning - tournament already started")
    (router/dispatch t-ctx (api/delete-team-event team-id))))


;--- subscriptions ---

(defn subscribe-teams [ctx]
  (ctx/subscribe-data ctx [:teams]))
