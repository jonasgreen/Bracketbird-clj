(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r])
  )


(defonce state (r/atom {:a 0}))


(defn on-js-reload
  "optionally touch your app-state to force rerendering depending on
  your application"
  []
  (print "waaasdfasdfaasdf" @state)
  (swap! state update :a inc))


(defn b[a]
  [:div (str "aaasdfasdfa" a)])

(defn- application[state]
  [:div [b (:a @state)]])

(defn main []
  (enable-console-print!)
  (r/render-component [application state] (js/document.getElementById "application")))
