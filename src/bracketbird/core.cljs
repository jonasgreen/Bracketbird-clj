(ns ^:figwheel-always sagsbehandler.core
  (:require [reagent.core :as r])
  (:import [goog Uri]))


(defn on-js-reload
  "optionally touch your app-state to force rerendering depending on
  your application"
  [])


(defn- application[]
  [:div "in-the-air"])

(defn main []
  (enable-console-print!)
  (r/render-component [application] (js/document.getElementById "application")))
