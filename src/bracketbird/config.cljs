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
            [bracketbird.dom :as d]))

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
              :subscribe [:hook/system]})

(def ui-application-page {:hook        :ui-application-page
                          :local-state {:active-page :ui-front-page}
                          :subscribe   [:hook/application]
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
                                                           :settings {:header "SETTINGS" :content settings-tab/render}
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
                   :subscribe        [:hook/teams-order :hook/teams]
                   :scroll-to-bottom (fn [handle _ _] (-> handle
                                                          (rc/get-element "table")
                                                          (ut/scroll-elm-to-bottom!)))

                   :focus-last-team  (fn [{:keys [ctx]} _ {:keys [hook/teams-order]}]
                                       (when (seq teams-order)
                                         (-> (merge ctx {:team-id (last teams-order)})
                                             (rc/get-handle :ui-team-row)
                                             (rc/dispatch :focus))))})

(def ui-team-row {:hook                      :ui-team-row
                  :render                    teams-tab/team-row
                  :subscribe                 [:hook/team]

                  :local-state               (fn [{:keys [hook/team]}]
                                               {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})

                  :update-team               (fn [h {:keys [input-value]} {:keys [hook/team]}]
                                               (when (rc/has-changed input-value (:team-name team))
                                                 (ui-services/dispatch-event
                                                   {:event-type [:team :update]
                                                    :ctx        (:ctx h)
                                                    :content    {:team-name input-value}})))

                  :delete-team               (fn [h ls {:keys [hook/team]}]
                                               (let [team-to-focus (or
                                                                     (ui-services/after-team h (:team-id team))
                                                                     (ui-services/previous-team h (:team-id team)))]
                                                 (ui-services/dispatch-event
                                                   {:event-type  [:team :delete]
                                                    :ctx         (assoc (:ctx h) :team-id (:team-id team))
                                                    :post-render (fn [_]
                                                                   (if team-to-focus
                                                                     (rc/focus h :ui-team-row :team-id team-to-focus)
                                                                     (rc/focus h :ui-enter-team-input)))})))
                  :focus                     (fn [h _ _] (-> h (rc/get-element "input") (.focus)))

                  :delete-icon-on-click      (fn [h _ _ e] (rc/dispatch h :delete-team))
                  :input-on-key-down         (fn [h _ {:keys [hook/team]} e]
                                               (d/handle-key e {:ESC            (fn [e] (rc/delete-local-state h) [:STOP-PROPAGATION])
                                                                :ENTER          (fn [_] (rc/dispatch h :update-team))
                                                                [:SHIFT :ENTER] (fn [e] (ui-services/dispatch-event
                                                                                          {:event-type  [:team :create]
                                                                                           :ctx         (:ctx h)
                                                                                           :content     {:team-name ""
                                                                                                         :index     (ui-services/index-of h (:team-id team))}
                                                                                           :post-render (fn [event]
                                                                                                          (-> (:ctx h)
                                                                                                              (assoc :team-id (:team-id event))
                                                                                                              (rc/get-handle :ui-team-row)
                                                                                                              (rc/dispatch :focus)))}))
                                                                :UP             (fn [_] (->> (:team-id team)
                                                                                             (ui-services/previous-team h)
                                                                                             (rc/focus h :ui-team-row :team-id)))
                                                                :DOWN           (fn [_] (let [team-to-focus (ui-services/after-team h (:team-id team))]
                                                                                          (if team-to-focus
                                                                                            (rc/focus h :ui-team-row :team-id team-to-focus)
                                                                                            (rc/focus h :ui-enter-team-input))))}))

                  :input-delete-on-backspace (fn [h _ _ e] (rc/dispatch h :delete-team))
                  :input-on-blur             (fn [h _ _ e] (rc/dispatch h :update-team))})



(def ui-enter-team-input {:hook                      :ui-enter-team-input
                          :render                    teams-tab/enter-team-input
                          :did-mount                 (fn [h _ _] (rc/dispatch h :focus))

                          :local-state               (fn [_] {:input-delete-on-backspace? true})
                          :create-team               (fn [{:keys [ctx] :as h} {:keys [input-value]} _]
                                                       (ui-services/dispatch-event
                                                         {:event-type     [:team :create]
                                                          :ctx            ctx
                                                          :content        {:team-name input-value}
                                                          :state-coeffect #(-> % (rc/update h dissoc :input-value))
                                                          :post-render    (fn [_]
                                                                            (-> (rc/get-handle ctx :ui-teams-tab)
                                                                                (rc/dispatch :scroll-to-bottom)))}))

                          :focus                     (fn [handle _ _] (-> handle (rc/get-element "input") (.focus)))

                          :input-on-key-down         (fn [h _ _ e]
                                                       (d/handle-key e {[:ENTER] (fn [_] (rc/dispatch h :create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                                                        [:UP]    (fn [_] (-> (:ctx h)
                                                                                             (rc/get-handle :ui-teams-tab)
                                                                                             (rc/dispatch :focus-last-team)))}))

                          :input-delete-on-backspace (fn [h _ _ _] (when-let [{:keys [team-name team-id] :as t} (ui-services/last-team h)]
                                                                     (when (string/blank? team-name)
                                                                       (ui-services/dispatch-event
                                                                         {:event-type  [:team :delete]
                                                                          :ctx         (assoc (:ctx h) :team-id team-id)
                                                                          :post-render (fn [_]
                                                                                         (-> (:ctx h)
                                                                                             (rc/get-handle :ui-teams-tab)
                                                                                             (rc/dispatch :scroll-to-bottom)))}))))
                          :button-on-click           (fn [h _ _ _]
                                                       #_(rc/dispatch h :create-team)
                                                       #_(rc/dispatch h :focus)
                                                       (swap! restyle.core/styles assoc :left (gensym))
                                                       (println restyle.core/styles)
                                                       )

                          :button-on-key-down        (fn [h _ _ e]
                                                       (d/handle-key e {[:ENTER] (fn [_]
                                                                                   (rc/dispatch h :create-team)
                                                                                   (rc/dispatch h :focus)
                                                                                   [:STOP-PROPAGATION :PREVENT-DEFAULT])}))})

(def ui-settings-tab {:hook   :ui-settings-tab
                      :render settings-tab/render})

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