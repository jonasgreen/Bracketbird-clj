(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r]
            [bracketbird.history :as history]
            [bracketbird.application-ui :as app]
            [bracketbird.application-controller :as app-ctrl]))

(defn on-js-reload []
  (app-ctrl/trigger-ui-reload))

(defn main []
  (enable-console-print!)
  (app-ctrl/enable-history)
  (r/render [app/render] (js/document.getElementById "application")))