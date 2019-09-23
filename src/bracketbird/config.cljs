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
            [bracketbird.components.ranking-tab :as ranking-tab]))

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
            :hook/group-match         [:hook/group :matches #{:match-id}]})


;UI - Containers

(def ui-root {:hook      :ui-root
              :render    pages/ui-root
              :reactions [:hook/system]})

(def ui-application-page {:hook        :ui-application-page
                          :local-state {:active-page :ui-front-page}
                          :reactions   [:hook/application]
                          :render      pages/ui-application-page})



(def ui-front-page {:hook              :ui-front-page
                    :render            pages/ui-front-page
                    :create-tournament (fn [handle _ _]
                                         (let [ctx (:ctx handle)
                                               tournament-id (system/unique-id :tournament)]
                                           (ui-services/dispatch-event
                                             {:event-type     [:tournament :create]
                                              :ctx            (assoc ctx :tournament-id tournament-id)
                                              :content        {:tournament-id tournament-id}
                                              :state-coeffect #(-> (rc/update % (rc/get-handle ctx :ui-application-page)
                                                                              assoc
                                                                              :active-page
                                                                              :ui-tournament-page))
                                              :post-render    (fn [_])})))})

(def ui-tournament-page {:hook        :ui-tournament-page
                         :render      pages/tournament-page
                         :local-state {:items             {:teams    {:header "TEAMS" :content :ui-teams-tab}
                                                           :settings {:header "SETTINGS" :content :ui-settings-tab}
                                                           :matches  {:header "MATCHES" :content :ui-matches-tab}
                                                           :ranking  {:header "SCORES" :content :ui-ranking-tab}}

                                       :order             [:teams :settings :matches :ranking]
                                       :selection-type    :single
                                       :selected          :teams
                                       :previous-selected :teams}

                         :select-item (fn [handle {:keys [selected]} _ select]
                                        (rc/put! handle assoc :previous-selected selected :selected select))})

(def ui-teams-tab {:hook             :ui-teams-tab
                   :render           teams-tab/render
                   :reactions        [:hook/teams-order :hook/teams]
                   :local-state      {:scroll-top    0
                                      :client-height 0
                                      :scroll-height 0}
                   :scroll-to-bottom (fn [handle _ _] (-> handle
                                                          (rc/get-element "scroll")
                                                          (ut/scroll-elm-to-bottom!)))

                   :focus-last-team  (fn [{:keys [ctx]} _ {:keys [hook/teams-order]}]
                                       (when (seq teams-order)
                                         (-> (merge ctx {:team-id (last teams-order)})
                                             (rc/get-handle :ui-team-row)
                                             (rc/dispatch :focus))))})

(def ui-team-row {:hook        :ui-team-row
                  :render      teams-tab/team-row
                  :reactions   [:hook/team]

                  :update-team (fn [handle {:keys [team-name]} _]
                                 (ui-services/dispatch-event
                                   {:event-type [:team :update]
                                    :ctx        (:ctx handle)
                                    :content    {:team-name team-name}}))

                  :focus       (fn [handle _ _] (-> handle (rc/get-element "team-name") (.focus)))})

(def ui-enter-team-input {:hook        :ui-enter-team-input
                          :render      teams-tab/enter-team-input
                          :did-mount   (fn [handle _ _] (-> handle (rc/get-element "input") (.focus)))

                          :create-team (fn [{:keys [ctx] :as handle} {:keys [team-name]} _]
                                         (let [start (.getTime (js/Date.))]
                                           (ui-services/dispatch-event
                                             {:event-type     [:team :create]
                                              :ctx            ctx
                                              :content        {:team-name team-name}
                                              :state-coeffect #(-> % (rc/update handle dissoc :team-name))
                                              :post-render    (fn [_]
                                                                (r/after-render #(println "time: " (- (.getTime (js/Date.)) start)))
                                                                (-> (rc/get-handle ctx :ui-teams-tab)
                                                                    (rc/dispatch :scroll-to-bottom)))})))})

(def ui-settings-tab {:hook        :ui-settings-tab
                      :render      settings-tab/render
                      :local-state {:scroll-top 0}})

(def ui-matches-tab {:hook   :ui-matches-tab
                     :render matches-tab/render})

(def ui-ranking-tab {:hook   :ui-ranking-tab
                     :render ranking-tab/render})


(def ui-layout [ui-root
                [ui-application-page #{:application-id}
                 [ui-front-page]
                 [ui-tournament-page #{:tournament-id}
                  [ui-teams-tab
                   [ui-team-row #{:team-id}]
                   [ui-enter-team-input]]
                  [ui-settings-tab]
                  [ui-matches-tab]
                  [ui-ranking-tab]]]])