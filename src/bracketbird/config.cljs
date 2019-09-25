(ns bracketbird.config
  (:require [recontain.core :as rc]
            [clojure.string :as string]
            [bracketbird.pages :as pages]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.components.teams-tab :as teams-tab]
            [bracketbird.system :as system]
            [bracketbird.util :as ut]
            [bracketbird.components.settings-tab :as settings-tab]
            [bracketbird.components.matches-tab :as matches-tab]
            [bracketbird.components.ranking-tab :as ranking-tab]
            [bracketbird.dom :as d]
            [reagent.core :as r]
            [bracketbird.rc-util :as rc-util]))

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

                  :local-state (fn [{:keys [hook/team]}]
                                 {:delete-by-backspace? (clojure.string/blank? (:team-name team))})

                  :update-team (fn [h {:keys [value]} _]
                                 (ui-services/dispatch-event
                                   {:event-type [:team :update]
                                    :ctx        (:ctx h)
                                    :content    {:team-name value}}))

                  :on-change   (fn [h ls fs e] (rc/put! h assoc :value (ut/value e)))
                  :on-key-down (fn [h _ {:keys [hook/team]} e]
                                 (d/handle-key e {:ESC  (fn [e] (rc/delete-local-state h) [:STOP-PROPAGATION])
                                                  :UP   (fn [_] (->> (:team-id team)
                                                                     (ui-services/previous-team h)
                                                                     (rc-util/focus h :ui-team-row :team-id)))

                                                  :DOWN (fn [_] (let [team-to-focus (ui-services/after-team h (:team-id team))]
                                                                  (if team-to-focus
                                                                    (rc-util/focus h :ui-team-row :team-id team-to-focus)
                                                                    (rc-util/focus h :ui-enter-team-input))))}))

                  :on-key-up   (fn [h _ _ e] (rc/put! h assoc :delete-by-backspace? (clojure.string/blank? (ut/value e))))

                  :focus       (fn [h _ _] (-> h (rc/get-element "team-name") (.focus)))})



(def ui-enter-team-input {:hook        :ui-enter-team-input
                          :render      teams-tab/enter-team-input
                          :did-mount   (fn [h _ _] (rc/dispatch h :focus))

                          :local-state (fn [_] {:delete-by-backspace? true})
                          :create-team (fn [{:keys [ctx] :as handle} {:keys [value]} _]
                                         (ui-services/dispatch-event
                                           {:event-type     [:team :create]
                                            :ctx            ctx
                                            :content        {:team-name value}
                                            :state-coeffect #(-> % (rc/update handle dissoc :value))
                                            :post-render    (fn [_]
                                                              (-> (rc/get-handle ctx :ui-teams-tab)
                                                                  (rc/dispatch :scroll-to-bottom)))}))

                          :on-key-up   (fn [h _ _ e] (rc/put! h assoc :delete-by-backspace? (clojure.string/blank? (ut/value e))))
                          :on-key-down (fn [h {:keys [delete-by-backspace?]} fs e]
                                         (d/handle-key e {[:ENTER]     (fn [_] (rc/dispatch h :create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                                          [:UP]        (fn [_] (-> (:ctx h)
                                                                                   (rc/get-handle :ui-teams-tab)
                                                                                   (rc/dispatch :focus-last-team)))

                                                          [:BACKSPACE] (fn [e] (when delete-by-backspace?
                                                                                 (let [{:keys [team-name team-id]} (ui-services/last-team h)]
                                                                                   (when (string/blank? team-name)
                                                                                     (ui-services/dispatch-event
                                                                                       {:event-type  [:team :delete]
                                                                                        :ctx         (assoc (:ctx h) :team-id team-id)
                                                                                        :post-render (fn [_]
                                                                                                       (-> (:ctx h)
                                                                                                           (rc/get-handle :ui-teams-tab)
                                                                                                           (rc/dispatch :scroll-to-bottom)))})))))}))
                          :on-change   (fn [h ls fs e] (rc/put! h assoc :value (ut/value e)))

                          :focus       (fn [handle _ _] (-> handle (rc/get-element "input") (.focus)))})

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