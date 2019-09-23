(ns ^:figwheel-hooks bracketbird.core
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [goog.events :as events]
            [goog.dom :as dom-helper]

            [bracketbird.state :as state]
            [bracketbird.system :as system]
            [bracketbird.config :as config]
            [bracketbird.dom :as d]
            [recontain.core :as rc]
            [bedrock.util :as b-ut]
            [bracketbird.pages :as pages]))

(defn component-hiccup-decorator [result {:keys [handle local-state foreign-states]}]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [_]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:hook handle)))
                                             (println (str "RENDER RESULT\n" (b-ut/pp-str result)))
                                             (println "HANDLE\n" (b-ut/pp-str handle))
                                             (println "LOCAL-STATE\n" (b-ut/pp-str local-state))
                                             (println "FOREIGN-STATE-KEYS\n" (b-ut/pp-str (keys foreign-states))))}
                        (:hook handle)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))


(defn mount-reagent []
  (r/render [rc/build nil {} pages/ui-root] (js/document.getElementById "system")))

(defn setup-recontain []
  (swap! state/state assoc :rc-config
         (rc/setup {:state-atom                 state/state
                    :data-hooks                 config/hooks
                    :ui-layout                  config/ui-layout
                    :debug?                     (system/debug?)
                    :component-hiccup-decorator (when (system/debug?) component-hiccup-decorator)})))

(defn main []
  (enable-console-print!)
  (swap! state/state assoc :system {:debug?             false
                                    :active-application nil
                                    :test               (or
                                                          (= (.. js/window -location -hostname) "localhost")
                                                          (= (.. js/window -location -hash) "#test"))})

  (setup-recontain)

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


  (events/listen js/window "keydown" (fn [e] (when (d/key-and-modifier? :D d/alt-modifier? e)
                                               (.stopPropagation e)
                                               (.preventDefault e)
                                               (swap! state/state update-in [:system :debug?] not))))
  )

(defn ^:after-load on-js-reload []
  (r/unmount-component-at-node (dom-helper/getElement "system"))
  (setup-recontain)
  (let [start (.getTime (js/Date.))]
    (r/after-render #(println "reload time: " (- (.getTime (js/Date.)) start)))
    (mount-reagent)))
