(ns bracketbird.control.tournament-api
  (:require [bracketbird.event-router :as event-router]
            [bracketbird.util :as ut]))


(defn create-team [{:keys [tournament-id] :as ctx} team-name]
  (event-router/dispatch ctx :create-team {:team-id   (ut/squuid)
                                           :team-name team-name}))