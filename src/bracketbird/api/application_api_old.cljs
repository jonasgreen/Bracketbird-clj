(ns bracketbird.api.application-api_old
  (:require [bracketbird.state :as app-state]))



(defn update-page-context [ctx]
  (swap! app-state/state assoc :page-context ctx))



(defn reload-ui []
  (swap! app-state/state update-in [:_system :figwheel-reloads] inc))