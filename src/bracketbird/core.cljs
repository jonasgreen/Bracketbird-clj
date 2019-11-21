(ns ^:figwheel-hooks bracketbird.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [airboss.core :as airboss]
            [goog.events :as events]
            [bracketbird.style :as styles]
            [bracketbird.state :as state]
            [bracketbird.system :as system]
            [bracketbird.config.application :as application-config]
            [bracketbird.config.matches-page :as matches-page-config]
            [bracketbird.config.ranking-page :as ranking-page-config]
            [bracketbird.config.settings-page :as setting-page-config]
            [bracketbird.config.teams-page :as teams-page-config]
            [bracketbird.config.components :as components]
            [bracketbird.config.decorations :as decorations]
            [recontain.impl.container :as container]
            [bracketbird.dom :as d]
            [recontain.core :as rc]
            [restyle.core :as rs]
            [bedrock.util :as b-ut]))

(defn component-hiccup-decorator [result {:keys [handle local-state foreign-states]}]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [_]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:config-name handle)))
                                             (println (str "RENDER RESULT\n" (b-ut/pp-str result)))
                                             (println "HANDLE\n" (b-ut/pp-str handle))
                                             (println "LOCAL-STATE\n" (b-ut/pp-str local-state))
                                             (println "FOREIGN-STATE-KEYS\n" (b-ut/pp-str (keys foreign-states))))}
                        (:config-name handle)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))


(defn mount-reagent []
  (r/render [rc/root :root] (js/document.getElementById "system")))

(defn setup-styles []
  (rs/setup styles/styles)
  (swap! state/state assoc :styles styles/styles))

(defn setup-recontain []
  (swap! state/state assoc :rc-config
         (rc/setup {:clear-container-state-on-unmount? (not system/test?)
                    :state-atom                        state/state
                    :decorations                       decorations/decorations
                    :elements                          components/elements
                    :components                        [application-config/root
                                                        application-config/application-page
                                                        application-config/front-page
                                                        application-config/tournament-page

                                                        ;; teams tab
                                                        teams-page-config/teams-page
                                                        teams-page-config/team-row
                                                        teams-page-config/add-team

                                                        setting-page-config/settings-page
                                                        setting-page-config/stage-component
                                                        setting-page-config/add-stage-buttons

                                                        matches-page-config/matches-page
                                                        ranking-page-config/ranking-page]

                    :debug?                            (system/debug?)
                    :component-hiccup-decorator        (when (system/debug?) component-hiccup-decorator)})))

(defn- reload-ui []
  (setup-styles)
  (setup-recontain)
  (let [start (.getTime (js/Date.))]
    (r/after-render #(println "reload time: " (- (.getTime (js/Date.)) start)))
    ;(r/force-update-all)
    (rc/reload-configurations)))

(defn main []
  (enable-console-print!)
  (swap! state/state assoc
         :system {:debug?             false
                  :active-application nil
                  :test               (or
                                        (= (.. js/window -location -hostname) "localhost")
                                        (= (.. js/window -location -hash) "#test"))})

  (setup-styles)
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

  (events/listen js/window "resize" (fn [e]
                                      #_(rc/force-render-all)
                                      #_(swap! state/state update :system assoc
                                               :window-height (.-innerHeight (.-target e))
                                               :window-width (.-innerWidth (.-target e)))))


  (events/listen js/window "keydown" (fn [e] (when (d/key-and-modifier? :D d/alt-modifier? e)
                                               (.stopPropagation e)
                                               (.preventDefault e)
                                               (r/after-render reload-ui)
                                               (swap! state/state update-in [:system :debug?] not)))))


(defn ^:after-load on-js-reload []
  (reload-ui))
