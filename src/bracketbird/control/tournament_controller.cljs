(ns bracketbird.tournament-controller
  (:require [bracketbird.context :as context]
            [bracketbird.model.tournament-state :as t-state]
            [bracketbird.model.tournament :as t]
            [bracketbird.event-router :as router]
            [bracketbird.model.team :as team-m]
            [bracketbird.util.utils :as ut]
            [bracketbird.tournament-api :as api]
            [bracketbird.application-state :as app-state]))


(defn mk-ctx [tournament-id]
  (context/mk :tournament tournament-id))

;--- utils ---

(defn count-tournaments []
  (count (get @app-state/state :tournament)))

(defn tournament [ctx]
  (-> ctx context/data))

(defn teams [ctx]
  (-> ctx tournament t/teams))

(defn last-team [ctx]
  (-> ctx teams last))

(defn last-team? [ctx team]
  (= team (last-team ctx)))

(defn next-team [ctx team]
  (-> ctx teams (ut/next-entity team)))

(defn previous-team [ctx team]
  (-> ctx teams (ut/previous-entity team)))


;--- api ---

(defn create-tournament [ctx]
  (->> (api/create-tournament-event (:id ctx))
       (router/dispatch ctx)))


(defn add-team [ctx name]
  (if (-> ctx context/data t-state/started?)
    (println "warning - tournament already started")
    (->> (api/add-team-event name)
         (router/dispatch ctx))))

(defn update-team-name [ctx team-id name]
  (->> (api/update-team-name-event team-id name)
       (router/dispatch ctx)))

(defn delete-team [ctx team]
  (if (-> ctx context/data t-state/started?)
    (println "warning - tournament already started")
    (router/dispatch ctx (api/delete-team-event (team-m/team-id team)))))



;--- subscriptions ---

(defn subscribe-teams [ctx]
  (context/subscribe-data ctx [:teams]))
