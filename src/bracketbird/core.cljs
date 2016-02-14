(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r]
            [bracketbird.model.event :as id])
  )


(defonce state (r/atom {:a 0}))


(defn on-js-reload
  "optionally touch your app-state to force rerendering depending on
  your application"
  []
  (print "waaasdfasdfaasdf" @state)
  (swap! state update :a inc))



(defn b [a]
  (let [i (id/squuid)]
    [:div (str "id " i "time " (js/Date. (id/squuid-time-millis i)))]))

(defn- application [state]
  [:div [b (:a @state)]])

(defn main []
  (enable-console-print!)
  (r/render-component [application state] (js/document.getElementById "application")))
