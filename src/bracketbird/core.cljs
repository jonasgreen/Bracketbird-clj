(ns ^:figwheel-hooks bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [bracketbird.state :as state]
            [bracketbird.pages.error-page :as error-page]
            [bracketbird.pages.tournament-page :as tournament-page]
            [bracketbird.pages.front-page :as front-page]
            [bracketbird.system :as system]))


(defn render-application [ctx]
  (let [application (state/subscribe :application ctx)]
    (fn [_]
      [:div {:class :application} (condp = (:active-page @application)
                                    :front-page [front-page/render ctx]
                                    :tournament-page [tournament-page/render (-> (state/query :tournament ctx)
                                                                                 (select-keys [:tournament-id])
                                                                                 (merge ctx))]
                                    [error-page/render])])))

(defn render-system [_]
  (println "start system")
  (let [system (state/subscribe [:system])
        applications (state/subscribe [:applications])]
    (println "applications" @applications)
    (fn [_]
      [:div {:class :system}
       (map (fn [id] ^{:key id} [render-application {:application-id id}]) (keys @applications))])))

(defn mount-reagent []
  (r/render [render-system] (js/document.getElementById "system")))

(defn main []
  (enable-console-print!)
  (system/initialize!)
  (mount-reagent)
  (airboss/load-state-viewer state/state)
  (airboss/load-design-viewer))


(defn ^:after-load on-js-reload []
  (mount-reagent))
