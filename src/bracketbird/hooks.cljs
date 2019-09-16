(ns bracketbird.hooks
  (:require [bracketbird.components.teams-tab :as teams-tab]
            [bracketbird.components.settings-tab :as settings-tab]
            [bracketbird.components.ranking-tab :as ranking-tab]
            [bracketbird.components.matches-tab :as matches-tab]
            [bracketbird.pages :as pages]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.ui :as ui]
            [bracketbird.util :as ut]))


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
                                        :render      pages/application
                                        :local-state {:active-page :hooks/ui-front-page}}

            :hooks/ui-front-page       {:path        [:hooks/ui-application-page :front-page]
                                        :render      pages/front
                                        :local-state {}

                                        :fns         {:create-tournament
                                                      (fn [_ _ f]
                                                        (ui-services/dispatch-event
                                                          {:event-type     [:tournament :create]
                                                           :ctx            (f :ctx)
                                                           :content        {}
                                                           :state-coeffect #(-> (f % :update :hooks/ui-application-page
                                                                                   assoc
                                                                                   :active-page
                                                                                   :hooks/ui-tournament-page))}))}}

            :hooks/ui-tournament-page  {:path        [:hooks/ui-application-page :tournament-page]
                                        :render      pages/tournament
                                        :local-state {:items             {:teams    {:header "TEAMS" :content :hooks/ui-teams-tab}
                                                                          :settings {:header "SETTINGS" :content :hooks/ui-settings-tab}
                                                                          :matches  {:header "MATCHES" :content :hooks/ui-matches-tab}
                                                                          :ranking  {:header "SCORES" :content :hooks/ui-ranking-tab}}

                                                      :order             [:teams :settings :matches :ranking]
                                                      :selection-type    :single
                                                      :selected          :teams
                                                      :previous-selected :teams}}
            ;; --- TEAMS TAB
            :hooks/ui-teams-tab        {:path        [:hooks/ui-tournament-page :teams-tab]
                                        :render      teams-tab/render
                                        :reactions   [:hooks/teams-order]
                                        :local-state {:scroll-top    0
                                                      :client-height 0
                                                      :scroll-height 0}}

            :hooks/ui-team-row         {:path      [:hooks/ui-teams-tab :team-id]
                                        :render    teams-tab/team-row
                                        :reactions [:hooks/team]}


            :hooks/ui-enter-team-input {:path   [:hooks/ui-teams-tab :enter-team-input]
                                        :render teams-tab/enter-team-input
                                        :fns    {:create-team (fn [{:keys [team-name]} _ h]
                                                                (ui-services/dispatch-event
                                                                  {:event-type     [:team :create]
                                                                   :ctx            (h :ctx)
                                                                   :content        {:team-name team-name}
                                                                   :state-coeffect #(-> % (h :update dissoc :team-name))
                                                                   :post-render    (fn [_]
                                                                                     (-> h
                                                                                         (ui/foreign-handle :hooks/ui-teams-tab)
                                                                                         (ui/get-element "scroll")
                                                                                         (ut/scroll-elm-to-end)))}))}}

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
