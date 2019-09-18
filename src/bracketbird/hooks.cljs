(ns bracketbird.hooks
  (:require [bracketbird.components.teams-tab :as teams-tab]
            [bracketbird.components.settings-tab :as settings-tab]
            [bracketbird.components.ranking-tab :as ranking-tab]
            [bracketbird.components.matches-tab :as matches-tab]
            [bracketbird.pages :as pages]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.hookit :as h]
            [bracketbird.util :as ut]
            [reagent.core :as r]))



(def hooks {:hooks/system              [:system]
            :hooks/applications        [:applications]
            :hooks/application         [:applications :application-id]
            :hooks/tournament          [:hooks/application :tournament]

            :hooks/teams               [:hooks/tournament :teams]
            :hooks/teams-order         [:hooks/tournament :teams-order]
            :hooks/team                [:hooks/tournament :teams :team-id]

            :hooks/stages              [:hooks/tournament :stages]
            :hooks/stages-order        [:hooks/tournament :stages-order]
            :hooks/stage               [:hooks/tournament :stages :stage-id]

            ;notice these are matches in a given stage
            :hooks/stage-matches       [:hooks/stage :matches]
            :hooks/stage-matches-order [:hooks/stage :matches :matches-order]
            :hooks/stage-match         [:hooks/stage :matches :match-id]

            :hooks/groups              [:hooks/stage :groups]
            :hooks/groups-order        [:hooks/stage :groups-order]
            :hooks/group               [:hooks/stage :groups :group-id]

            :hooks/group-matches       [:hooks/group :matches]
            :hooks/group-matches-order [:hooks/group :matches :matches-order]
            :hooks/group-match         [:hooks/group :matches :match-id]

            ;; UI
            :hooks/ui-root             {:path      [:ui]
                                        :render    pages/ui-root
                                        :reactions [:hooks/system]}

            :hooks/ui-application-page {:path        [:ui :applications :application-id]
                                        :render      pages/application-page
                                        :local-state {:active-page :hooks/ui-front-page}
                                        :reactions   [:hooks/application]}

            :hooks/ui-front-page       {:path              [:hooks/ui-application-page :front-page]
                                        :render            pages/front-page
                                        :local-state       {}
                                        :create-tournament (fn [handle _ _]
                                                             (ui-services/dispatch-event
                                                               {:event-type     [:tournament :create]
                                                                :ctx            (:ctx handle)
                                                                :content        {}
                                                                :state-coeffect #(-> (h/update % (h/get-handle handle :hooks/ui-application-page) assoc
                                                                                               :active-page
                                                                                               :hooks/ui-tournament-page))
                                                                :post-render    (fn [_])}))}

            :hooks/ui-tournament-page  {:path        [:hooks/ui-application-page :tournament-page]
                                        :render      pages/tournament-page
                                        :local-state {:items             {:teams    {:header "TEAMS" :content :hooks/ui-teams-tab}
                                                                          :settings {:header "SETTINGS" :content :hooks/ui-settings-tab}
                                                                          :matches  {:header "MATCHES" :content :hooks/ui-matches-tab}
                                                                          :ranking  {:header "SCORES" :content :hooks/ui-ranking-tab}}

                                                      :order             [:teams :settings :matches :ranking]
                                                      :selection-type    :single
                                                      :selected          :teams
                                                      :previous-selected :teams}

                                        :select-item (fn [handle {:keys [selected]} _ select]
                                                       (h/put! handle assoc :previous-selected selected :selected select))

                                        }
            ;; --- TEAMS TAB
            :hooks/ui-teams-tab        {:path             [:hooks/ui-tournament-page :teams-tab]
                                        :render           teams-tab/render
                                        :reactions        [:hooks/teams-order :hooks/teams]
                                        :local-state      {:scroll-top    0
                                                           :client-height 0
                                                           :scroll-height 0}
                                        :scroll-to-bottom (fn [handle _ _] (-> handle
                                                                               (h/get-element "scroll")
                                                                               (ut/scroll-elm-to-bottom!)))

                                        :focus-last-team  (fn [handle {:keys [hooks/teams-order]} _]
                                                            (-> handle
                                                                (h/get-handle :hooks/ui-team-row {:team-id (last teams-order)})
                                                                (h/dispatch :focus)))}

            :hooks/ui-team-row         {:path        [:hooks/ui-teams-tab :team-id]
                                        :render      teams-tab/team-row
                                        :reactions   [:hooks/team]

                                        :update-team (fn [handle {:keys [team-name]} _]
                                                       (ui-services/dispatch-event
                                                         {:event-type [:team :update]
                                                          :ctx        (:ctx handle)
                                                          :content    {:team-name team-name}}))

                                        :focus       (fn [handle _ _] (-> handle (h/get-element "team-name") (.focus)))}


            :hooks/ui-enter-team-input {:path        [:hooks/ui-teams-tab :enter-team-input]
                                        :render      teams-tab/enter-team-input
                                        :did-mount   (fn [handle _ _] (-> handle (h/get-element "input") (.focus)))

                                        :create-team (fn [handle {:keys [team-name]} _]
                                                       (let [start (.getTime (js/Date.))]
                                                         (ui-services/dispatch-event
                                                           {:event-type     [:team :create]
                                                            :ctx            (:ctx handle)
                                                            :content        {:team-name team-name}
                                                            :state-coeffect #(-> % (h/update handle dissoc :team-name))
                                                            :post-render    (fn [_]
                                                                              (r/after-render #(println "time: " (- (.getTime (js/Date.)) start)))
                                                                              (-> handle
                                                                                  (h/get-handle :hooks/ui-teams-tab)
                                                                                  (h/dispatch :scroll-to-bottom)))})))}

            ;; --- SETTINGS TAB
            :hooks/ui-settings-tab     {:path        [:hooks/ui-tournament-page :settings-tab]
                                        :render      settings-tab/render
                                        :local-state {:scroll-top 0}}
            ;; --- MATCHES TAB
            :hooks/ui-matches-tab      {:path   [:hooks/ui-tournament-page :matches-tab]
                                        :render matches-tab/render}
            ;; --- RANKING TAB
            :hooks/ui-ranking-tab      {:path   [:hooks/ui-tournament-page :ranking-tab]
                                        :render ranking-tab/render}})
