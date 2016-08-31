(ns bracketbird.api.application-api
  (:require [bracketbird.application-state :as app-state]))



(defn update-page-context [ctx]
  (swap! app-state/state assoc :page-context ctx))


(defn reload-ui []
  (swap! app-state/state update-in [:system :figwheel-reloads] inc))