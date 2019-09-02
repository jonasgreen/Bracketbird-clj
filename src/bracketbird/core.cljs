(ns ^:figwheel-hooks bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [bracketbird.state :as state]
            [bracketbird.pages.error-page :as error-page]
            [bracketbird.pages.tournament-page :as tournament-page]
            [bracketbird.pages.front-page :as front-page]
            [bracketbird.system :as system]
            [bracketbird.ui :as ui]))


(defn render-application [ctx]
  (let [application (state/hook :application ctx)]
    (fn [_ _]
      [:div {:class :application} (condp = (:active-page @application)
                                    :front-page [front-page/render ctx]
                                    :tournament-page [tournament-page/render (-> (:tournament @application)
                                                                                 (select-keys [:tournament-id])
                                                                                 (merge ctx))]
                                    [error-page/render])])))

(defn render-system [_]
  (let [system (state/hook :system {})]
    (fn [_]
      [:div {:class :system}
       (when-let [id (:active-application @system)]
         [render-application {:application-id id}])])))

(defn mount-reagent []
  (r/render [render-system] (js/document.getElementById "system")))

(defn main []
  (enable-console-print!)

  (swap! state/state assoc :hooks state/hooks-spc)

  (system/initialize!)
  (mount-reagent)
  (airboss/load-state-viewer state/state)
  (airboss/load-design-viewer))


(defn ^:after-load on-js-reload []
  (mount-reagent))
