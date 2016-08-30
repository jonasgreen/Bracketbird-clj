(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [bracketbird.application-ui :as app]
            [bracketbird.app-state :as app-state]
            [bracketbird.application-controller :as app-ctrl])

  (:import [goog Uri]
           [goog.history Html5History]))



(defn on-js-reload
  "optionally touch your app-state to force rerendering depending on
  your application"
  []
  (swap! app-state/state update :figwheel-reloads inc))




(defn main []
  (enable-console-print!)
  (println "main")


  (let [history (Html5History. js/window)]
    (events/listen history EventType/NAVIGATE #(app-ctrl/on-navigation-changed %))
    (.setEnabled history true))

  (r/render [app/render app-state/state] (js/document.getElementById "application")))
