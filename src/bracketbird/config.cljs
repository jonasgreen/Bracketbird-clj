(ns bracketbird.config
  (:require [recontain.core :as rc]
            [reagent.core :as r]
            [bracketbird.pages :as pages]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.components.teams-tab :as teams-tab]
            [bracketbird.system :as system]
            [bracketbird.util :as ut]
            [bracketbird.components.settings-tab :as settings-tab]
            [bracketbird.components.matches-tab :as matches-tab]
            [bracketbird.components.ranking-tab :as ranking-tab]
            [bedrock.util :as b-ut]))

(def hooks {:hook/system              [:system]
            :hook/applications        [:applications]
            :hook/application         [:applications #{:application-id}]
            :hook/tournament          [:hook/application :tournaments #{:tournament-id}]

            :hook/teams               [:hook/tournament :teams]
            :hook/teams-order         [:hook/tournament :teams-order]
            :hook/team                [:hook/tournament :teams #{:team-id}]

            :hook/stages              [:hook/tournament :stages]
            :hook/stages-order        [:hook/tournament :stages-order]
            :hook/stage               [:hook/tournament :stages #{:stage-id}]

            ;notice these are matches in a given stage
            :hook/stage-matches       [:hook/stage :matches]
            :hook/stage-matches-order [:hook/stage :matches :matches-order]
            :hook/stage-match         [:hook/stage :matches #{:match-id}]

            :hook/groups              [:hook/stage :groups]
            :hook/groups-order        [:hook/stage :groups-order]
            :hook/group               [:hook/stage :groups #{:group-id}]

            :hook/group-matches       [:hook/group :matches]
            :hook/group-matches-order [:hook/group :matches :matches-order]
            :hook/group-match         [:hook/group :matches #{:match-id}]

            ;ui hooks
            :hook/ui-root             {:path      [:ui]
                                       :render    pages/ui-root
                                       :reactions [:hook/system]}

            :hook/ui-application-page {:path        [:ui :applications #{:application-id}]
                                       :render      pages/application-page
                                       :local-state {:active-page :hook/ui-front-page}
                                       :reactions   [:hook/application]}

            :hook/ui-front-page       {:path              [:hook/ui-application-page :front-page]
                                       :render            pages/front-page
                                       :local-state       {}

                                       :create-tournament (fn [handle _ _]
                                                            (let [tournament-id (system/unique-id :tournament)]
                                                              (ui-services/dispatch-event
                                                                {:event-type     [:tournament :create]
                                                                 :ctx            (assoc (:ctx handle) :tournament-id tournament-id)
                                                                 :content        {:tournament-id tournament-id}
                                                                 :state-coeffect #(-> (rc/update % (rc/get-handle handle :hook/ui-application-page) assoc
                                                                                                 :active-page
                                                                                                 :hook/ui-tournament-page))
                                                                 :post-render    (fn [_])})))}

            :hook/ui-tournament-page  {:path        [:hook/ui-application-page :tournaments #{:tournament-id}]
                                       :render      pages/tournament-page
                                       :local-state {:items             {:teams    {:header "TEAMS" :content :hook/ui-teams-tab}
                                                                         :settings {:header "SETTINGS" :content :hook/ui-settings-tab}
                                                                         :matches  {:header "MATCHES" :content :hook/ui-matches-tab}
                                                                         :ranking  {:header "SCORES" :content :hook/ui-ranking-tab}}

                                                     :order             [:teams :settings :matches :ranking]
                                                     :selection-type    :single
                                                     :selected          :teams
                                                     :previous-selected :teams}

                                       :select-item (fn [handle {:keys [selected]} _ select]
                                                      (rc/put! handle assoc :previous-selected selected :selected select))

                                       }
            ;; --- TEAMS TAB
            :hook/ui-teams-tab        {:path             [:hook/ui-tournament-page :teams-tab]
                                       :render           teams-tab/render
                                       :reactions        [:hook/teams-order :hook/teams]
                                       :local-state      {:scroll-top    0
                                                          :client-height 0
                                                          :scroll-height 0}
                                       :scroll-to-bottom (fn [handle _ _] (-> handle
                                                                              (rc/get-element "scroll")
                                                                              (ut/scroll-elm-to-bottom!)))

                                       :focus-last-team  (fn [handle _ {:keys [hook/teams-order]}]
                                                           (when (seq teams-order)
                                                             (-> handle
                                                                 (rc/get-handle :hook/ui-team-row {:team-id (last teams-order)})
                                                                 (rc/dispatch :focus))))}

            :hook/ui-team-row         {:path        [:hook/ui-teams-tab #{:team-id}]
                                       :render      teams-tab/team-row
                                       :reactions   [:hook/team]

                                       :update-team (fn [handle {:keys [team-name]} _]
                                                      (ui-services/dispatch-event
                                                        {:event-type [:team :update]
                                                         :ctx        (:ctx handle)
                                                         :content    {:team-name team-name}}))

                                       :focus       (fn [handle _ _] (-> handle (rc/get-element "team-name") (.focus)))}


            :hook/ui-enter-team-input {:path        [:hook/ui-teams-tab :enter-team-input]
                                       :render      teams-tab/enter-team-input
                                       :did-mount   (fn [handle _ _] (-> handle (rc/get-element "input") (.focus)))

                                       :create-team (fn [handle {:keys [team-name]} _]
                                                      (let [start (.getTime (js/Date.))]
                                                        (ui-services/dispatch-event
                                                          {:event-type     [:team :create]
                                                           :ctx            (:ctx handle)
                                                           :content        {:team-name team-name}
                                                           :state-coeffect #(-> % (rc/update handle dissoc :team-name))
                                                           :post-render    (fn [_]
                                                                             (r/after-render #(println "time: " (- (.getTime (js/Date.)) start)))
                                                                             (-> handle
                                                                                 (rc/get-handle :hook/ui-teams-tab)
                                                                                 (rc/dispatch :scroll-to-bottom)))})))}

            ;; --- SETTINGS TAB
            :hook/ui-settings-tab     {:path        [:hook/ui-tournament-page :settings-tab]
                                       :render      settings-tab/render
                                       :local-state {:scroll-top 0}}
            ;; --- MATCHES TAB
            :hook/ui-matches-tab      {:path   [:hook/ui-tournament-page :matches-tab]
                                       :render matches-tab/render}
            ;; --- RANKING TAB
            :hook/ui-ranking-tab      {:path   [:hook/ui-tournament-page :ranking-tab]
                                       :render ranking-tab/render}})


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

(defn setup-recontain [state-atom]
  (swap! state-atom assoc :rc-config
         (rc/setup {:state-atom                 state-atom
                    :hooks                      hooks
                    :debug?                     (system/debug?)
                    :component-hiccup-decorator (when (system/debug?) component-hiccup-decorator)})))
