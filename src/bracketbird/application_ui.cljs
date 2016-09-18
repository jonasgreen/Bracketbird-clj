(ns bracketbird.application-ui
  (:require
    [goog.events :as events]
    [reagent.core :as r]

    [bracketbird.util.keyboard :as k]
    [bracketbird.application-controller :as app-ctrl]
    [bracketbird.pages.front-page :as front-page]
    [bracketbird.pages.tournament-page :as tournament-page]
    [bracketbird.pages.error-page :as error-page]
    [bracketbird.ui.state-viewer :as state-view]
    [bracketbird.ui.design-view :as design-view]))


;Used indirectly - toggles stateviewer. Don't remove ;)
(defonce state-view-listener-key (events/listen js/window "keydown"
                                                (fn [e]
                                                  (cond
                                                    (k/F10? e) (app-ctrl/toggle-design-tool)
                                                    (k/F12? e) (app-ctrl/toggle-state-tool)
                                                    (k/esc? e) (app-ctrl/close-tools)))))


(defn render [_]
  (let [{:keys [page ctx]} @(app-ctrl/subscribe-page-context)
        system @(app-ctrl/subscribe-system)]
    [:div
    (condp = page
      :front-page [front-page/render ctx]
      :tournament-page [tournament-page/render ctx]
      [error-page/render ctx])

     (when (:show-state? system)
       [state-view/render (app-ctrl/get-state-atom)])

     (when (:show-design? system)
       [design-view/render])]))