(ns ^:figwheel-always bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [bracketbird.application-state :as app-state]
            [bracketbird.application-controller :as app-ctrl]
            [bracketbird.pages.error-page :as error-page]
            [bracketbird.new-context :as new-context]
            [bracketbird.pages.tournament-page :as tournament-page]
            [bracketbird.pages.front-page :as front-page]))

(defn on-js-reload []
  (app-ctrl/trigger-ui-reload))

(defn render [_]
  (let [{:keys [page ctx]} @(app-ctrl/subscribe-page-context)]
    [:div
     (condp = page
       :front-page [front-page/render ctx]
       :tournament-page [tournament-page/render ctx {} (new-context/add-context {} :tournament-id (:ctx-id ctx))]
       [error-page/render ctx])]))

(defn main []
  (enable-console-print!)
  (app-ctrl/enable-history)
  (r/render [render] (js/document.getElementById "application"))

  (airboss/load-state-viewer app-state/state)
  (airboss/load-design-viewer))