(ns bracketbird.control.tournament-controller
  (:require [bracketbird.old-context :as context]
            [bracketbird.model.tournament-state :as t-state]
            [bracketbird.model.tournament :as t]
            [bracketbird.model.entity :as e]
            [bracketbird.model.team :as team-m]
            [bracketbird.api.tournament-api_old :as api]
            [bracketbird.state :as app-state]))


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
  (-> (last-team ctx)
      (e/same? team)))

(defn index-of-team [ctx team]
  (-> ctx
      teams
      (e/index-of-entity team)))

(defn next-team [ctx team]
  (-> ctx teams (e/next-entity team)))

(defn previous-team [ctx team]
  (-> ctx teams (e/previous-entity team)))


;--- api ---

(defn create-tournament [ctx]
  (->> (api/create-tournament-event (:id ctx))
       #_(router/dispatch-old ctx)))

(defn insert-team [ctx name index]
  (if (-> ctx context/data t-state/started?)
    (println "warning - tournament already started")
    (->> (api/insert-team-event name index)
         #_(router/dispatch-old ctx))))


(defn add-team [ctx name]
  (if (-> ctx context/data t-state/started?)
    (println "warning - tournament already started")
    (->> (api/add-team-event name)
         #_(router/dispatch-old ctx))))



(defn delete-team [ctx team]
  (if (-> ctx context/data t-state/started?)
    (println "warning - tournament already started")
    #_(router/dispatch-old ctx (api/delete-team-event (team-m/team-id team)))))



;--- subscriptions ---

(defn subscribe-teams [ctx]
  (context/subscribe-data ctx [:teams]))
