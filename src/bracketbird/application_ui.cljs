(ns bracketbird.application-ui
  (:require
    [goog.events :as events]
    [bracketbird.application-controller :as app-ctrl]
    [bracketbird.pages.front-page :as front-page]
    [bracketbird.pages.tournament-page :as tournament-page]
    [bracketbird.pages.error-page :as error-page]
    [bracketbird.ui.state-viewer :as state-view]
    [bracketbird.application-state :as state]
    [bracketbird.ui.design-view :as design-view]
    [bracketbird.util.keyboard :as k]))


;Used indirectly - toggles stateviewer. Don't remove ;)
(defonce state-view-listener-key (events/listen js/window "keydown"
                                                (fn [e]
                                                  (cond
                                                    (k/F10? e) (swap! state/state update-in [:system :show-design?] not)
                                                    (k/F12? e) (swap! state/state update-in [:system :show-state?] not)
                                                    (k/esc? e) (swap! state/state update-in [:system] assoc :show-state? false :show-design? false)))))


(defn render [_]
  (let [{:keys [page ctx]} @(app-ctrl/subscribe-page-context)
        system @(app-ctrl/subscribe-system)]
    (println "page" page)
    [:div
    (condp = page
      :front-page [front-page/render ctx]
      :tournament-page [tournament-page/render ctx]
      [error-page/render ctx])

     (when (:show-state? system)
       [state-view/render state/state])

     (when (:show-design? system)
       [design-view/render])]))