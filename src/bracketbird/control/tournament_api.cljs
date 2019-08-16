(ns bracketbird.control.tournament-api
  (:require [bracketbird.control.event-router :as event-router]
            [bracketbird.util :as ut]))


(defn create-tournament []
  (let [id (ut/squuid)]
    (event-router/dispatch {} :create-tournament {:tournament-id id})
    id))

(defn create-team [ctx team-name]
  (event-router/dispatch ctx :create-team {:team-id   (ut/squuid)
                                           :team-name team-name}))