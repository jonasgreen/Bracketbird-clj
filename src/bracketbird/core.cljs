(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r]
            [bracketbird.model.uuid :as id]
            [bracketbird.ui.frontpage :as front-page]
            [bracketbird.app-state :as app-state]))



(defn on-js-reload
  "optionally touch your app-state to force rerendering depending on
  your application"
  []
  (swap! app-state/state update :figwheel-reloads inc))


(defn render-application []
  [front-page/render @app-state/state])

(defn main []
  (enable-console-print!)
  (r/render [render-application] (js/document.getElementById "application")))
