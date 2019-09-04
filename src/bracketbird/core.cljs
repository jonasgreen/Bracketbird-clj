(ns ^:figwheel-hooks bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [goog.events :as events]
            [bracketbird.state :as state]
            [bracketbird.ui :as ui]
            [bracketbird.specification :as specification]
            [bracketbird.system :as system]))


(defn mount-reagent []
  (r/render [ui/gui :hooks/ui-system-page {}] (js/document.getElementById "system")))


(defn main []
  (enable-console-print!)

  (swap! state/state assoc :system {:active-application nil
                                    :test               (or
                                                          (= (.. js/window -location -hostname) "localhost")
                                                          (= (.. js/window -location -hash) "#test"))})

  (swap! state/state assoc :hooks specification/hooks)
  (swap! state/state assoc :renders specification/renders)


  (let [app-id (system/unique-id :application)]
    (swap! state/state assoc-in [:system :active-application] app-id)
    (swap! state/state assoc-in [:applications app-id] (system/mk-application app-id)))


  (mount-reagent)
  (airboss/load-state-viewer state/state)
  (airboss/load-design-viewer)


  (swap! state/state update :system assoc
         :window-height (.-innerHeight js/window)
         :window-width (.-innerWidth js/window))

  (events/listen js/window "resize" (fn [e] (swap! state/state update :system assoc
                                                   :window-height (.-innerHeight (.-target e))
                                                   :window-width (.-innerWidth (.-target e)))))

  )




(defn ^:after-load on-js-reload []
  (mount-reagent))
