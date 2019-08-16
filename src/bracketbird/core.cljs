(ns ^:figwheel-hooks bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [bracketbird.state :as state]
            [bracketbird.control.application-controller :as app-ctrl]
            [bracketbird.pages.error-page :as error-page]
            [bracketbird.pages.tournament-page :as tournament-page]
            [bracketbird.pages.front-page :as front-page]
            [bracketbird.state :as app-state]))


;; and this is what figwheel calls after each save
;(defn ^:after-load re-render []
  ;  (mount)
 ; )

(defn ^:after-load on-js-reload []
  (swap! app-state/state update-in [:pages :values :figwheel-reloads] inc))

(defn render [_]
  (let [pages (state/subscribe :pages {})]
    (fn [_]
      (let [{:keys [active-page ctx]} @pages]
        (println "RENDER CORE" + "asdf")
        [:div
         (condp = active-page
           :front-page [front-page/render ctx]
           :tournament-page [tournament-page/render ctx]
           [error-page/render ctx])]))))

(defn main []
  (enable-console-print!)
  (app-ctrl/enable-history)
  (r/render [render] (js/document.getElementById "application"))

  (airboss/load-state-viewer state/state)
  (airboss/load-design-viewer))

(println "læasjdfælaksj")