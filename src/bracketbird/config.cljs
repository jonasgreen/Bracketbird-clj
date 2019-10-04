(ns bracketbird.config
  (:require [recontain.core :as rc]
            [clojure.string :as string]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.system :as system]
            [bracketbird.util :as ut]
            [bracketbird.components.settings-tab :as settings-tab]
            [bracketbird.components.matches-tab :as matches-tab]
            [bracketbird.components.ranking-tab :as ranking-tab]
            [bracketbird.dom :as d]
            [bracketbird.style :as s]
            [restyle.core :as rs]))

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
              :render    (fn [h _ {:keys [hook/system]}]
                           (let [app-id (:active-application system)]
                             [:div
                              (if app-id
                                [rc/container {:application-id app-id} :ui-application-page]
                                [:div "No application"])]))

              :subscribe [:hook/system]})

(def ui-application-page {:hook        :ui-application-page
                          :local-state {:active-page :ui-front-page}
                          :subscribe   [:hook/application]
                          :render      (fn [h {:keys [active-page]} {:keys [hook/application]}]
                                         (condp = active-page
                                           :ui-front-page ^{:key 1} [rc/container {} :ui-front-page]
                                           :ui-tournament-page ^{:key 2} (let [tournament-id (-> application :tournaments keys first)]
                                                                           [rc/container {:tournament-id tournament-id} :ui-tournament-page])
                                           [:div "page " active-page " not supported"]))})


(def ui-front-page {:hook              :ui-front-page
                    :render            (fn [h _ _]
                                         [:div
                                          [:div {:style {:display :flex :justify-content :center :padding-top 30}}
                                           ;logo
                                           [:div {:style {:width 900}}
                                            [:div {:style {:letter-spacing 0.8 :font-size 22}}
                                             [:span {:style {:color "lightblue"}} "BRACKET"]
                                             [:span {:style {:color "#C9C9C9"}} "BIRD"]]]]

                                          [:div {:style {:display :flex :flex-direction :column :align-items :center}}
                                           [:div {:style {:font-size 48 :padding "140px 0 30px 0"}}
                                            "Instant tournaments"]
                                           [:button {:class    "largeButton primaryButton"
                                                     :on-click (fn [_] (rc/dispatch h :create-tournament))}

                                            "Create a tournament"]
                                           [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])

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

(def ui-tournament-page {:hook                  :ui-tournament-page
                         :render                (fn [_ {:keys [order items]} _]
                                                  [::page
                                                   [::menu (map (fn [k] ^{:key k} [::menu-item {:current/item k} (get-in items [k :header])]) order)]
                                                   (->> items
                                                        (reduce-kv (fn [m k {:keys [content]}]
                                                                     (conj m ^{:key k} [::content-holder {:current/item k} [rc/container {} content]]))
                                                                   [])
                                                        seq)])


                         :style                 (fn [h {:keys [selected order items current/item]} _]
                                                  {:page           {:height         "100vh"
                                                                    :display        :flex
                                                                    :flex-direction :column}

                                                   :menu           {:font-size      22
                                                                    :display        :flex
                                                                    :align-items    :center
                                                                    :min-height     [:app-padding]
                                                                    :padding-left   [:app-padding]
                                                                    :letter-spacing 1.2
                                                                    :padding-right  [:app-padding]}

                                                   :menu-item      (merge
                                                                     {:margin-right [:layout-unit]
                                                                      :opacity      0.5
                                                                      :cursor       :pointer}
                                                                     (when (= selected item) {:opacity 1 :cursor :auto}))

                                                   :content-holder (merge {:height :100%} (when-not (= selected item) {:display :none}))})

                         [:menu-item :on-click] (fn [h {:keys [current/item]} _ e]
                                                  (println "current item" item)
                                                  (rc/dispatch h :select-item item))

                         :local-state           {:items             {:teams    {:header "TEAMS" :content :ui-teams-tab}
                                                                     :settings {:header "SETTINGS" :content settings-tab/render}
                                                                     :matches  {:header "MATCHES" :content :ui-matches-tab}
                                                                     :ranking  {:header "SCORES" :content :ui-ranking-tab}}

                                                 :order             [:teams :settings :matches :ranking]
                                                 :selection-type    :single
                                                 :selected          :teams
                                                 :previous-selected :teams}

                         :select-item           (fn [handle {:keys [selected]} _ select]
                                                  (rc/put! handle assoc :previous-selected selected :selected select))})

(def ui-teams-tab {:hook             :ui-teams-tab
                   :render           (fn [h ls {:keys [hook/teams-order hook/teams]}]
                                       [::tab-content
                                        [::table
                                         (map (fn [team-id index]
                                                ^{:key team-id} [rc/container {:team-id team-id} :ui-team-row index]) teams-order (range (count teams)))]

                                        ; input field
                                        [rc/container {} :ui-enter-team-input]])

                   :style            (fn [h {:keys [table-scroll-top table-scroll-bottom]} fs]
                                       {:tab-content (merge {:display        :flex
                                                             :flex-direction :column
                                                             :height         :100%}
                                                            (when (< 0 table-scroll-top) {:border-top [:border]}))

                                        :table       (merge {:padding-top    [:layout-unit]
                                                             :max-height     :100%
                                                             :min-height     :200px
                                                             :padding-bottom [:layout-unit]
                                                             :overflow-y     :auto}
                                                            (when (< 0 table-scroll-bottom) {:border-bottom [:border]}))})

                   :subscribe        [:hook/teams-order :hook/teams]
                   :scroll-to-bottom (fn [handle _ _] (-> handle
                                                          (rc/get-element :table)
                                                          (ut/scroll-elm-to-bottom!)))

                   :focus-last-team  (fn [{:keys [ctx]} _ {:keys [hook/teams-order]}]
                                       (when (seq teams-order)
                                         (-> (merge ctx {:team-id (last teams-order)})
                                             (rc/get-handle :ui-team-row)
                                             (rc/dispatch :focus))))})




(def ui-team-row {:hook                             :ui-team-row
                  :subscribe                        [:hook/team]

                  :local-state                      (fn [{:keys [hook/team]}]
                                                      {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})

                  :render                           (fn [h {:keys [team-name-value] :as ls} {:keys [hook/team]} index]
                                                      [::row
                                                       [::icons
                                                        [ut/icon (rc/bind-options :delete-icon) "clear"]]
                                                       [::space]
                                                       [::seeding (inc index)]
                                                       [::team-name {:elm   :input
                                                                     :type  :text
                                                                     :value (if team-name-value team-name-value (:team-name team))}]])

                  :style                            (fn [h {:keys [icons-hover? row-hover?]} _]
                                                      {:row         {:display :flex :align-items :center :min-height [:row-height]}

                                                       :icons       {:display         :flex
                                                                     :align-items     :center
                                                                     :height          [:row-height]
                                                                     :justify-content :center
                                                                     :cursor          (if icons-hover? :pointer :normal)
                                                                     :width           [:app-padding]}

                                                       :delete-icon (merge {:font-size 8 :opacity 0.5 :transition "background 0.2s, color 0.2s, border-radius 0.2s"}
                                                                           (when-not row-hover?
                                                                             {:color :transparent})

                                                                           (when icons-hover?
                                                                             {:font-weight   :bold
                                                                              :background    :red
                                                                              :color         :white
                                                                              :font-size     10
                                                                              :border-radius 8}))
                                                       :space       {:width [:page-padding]}
                                                       :seeding     {:display :flex :align-items :center :width 30 :opacity 0.5 :font-size 10}
                                                       :team-name   (merge s/input-text-field {:min-width 200})})


                  :update-team                      (fn [h {:keys [team-name-value]} {:keys [hook/team]}]
                                                      (when (rc/has-changed team-name-value (:team-name team))
                                                        (ui-services/dispatch-event
                                                          {:event-type [:team :update]
                                                           :ctx        (:ctx h)
                                                           :content    {:team-name team-name-value}})))

                  :delete-team                      (fn [h ls {:keys [hook/team]}]
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
                  :focus                            (fn [h _ _] (-> h (rc/get-element :team-name) (.focus)))




                  [:delete-icon :on-click]          (fn [h _ _ e] (rc/dispatch h :delete-team))



                  [:team-name :on-key-down]         (fn [h _ {:keys [hook/team]} e]
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

                  [:team-name :delete-on-backspace] (fn [h _ _ e] (rc/dispatch h :delete-team))
                  [:team-name :on-blur]             (fn [h _ _ e] (rc/dispatch h :update-team))})



(def ui-enter-team-input {:hook                         :ui-enter-team-input

                          :render                       (fn [h {:keys [input-value] :as _} _]
                                                          [::row
                                                           [::input {:placeholder "Enter team"
                                                                     :type        :text
                                                                     :elm         :input
                                                                     :value       input-value}]

                                                           [::button {:class "primaryButton"} "Add Team"]])

                          :style                        (fn [h ls fs]
                                                          {:row   {:padding-left [+ :app-padding :page-padding]
                                                                   :display      :flex
                                                                   :min-height   [:app-padding]
                                                                   :align-items  :center}

                                                           :input {:border  :none
                                                                   :padding 0}})

                          :did-mount                    (fn [h _ _] (rc/dispatch h :focus))

                          :local-state                  (fn [_] {:input-delete-on-backspace? true})
                          :create-team                  (fn [{:keys [ctx] :as h} {:keys [input-value]} _]
                                                          (ui-services/dispatch-event
                                                            {:event-type     [:team :create]
                                                             :ctx            ctx
                                                             :content        {:team-name input-value}
                                                             :state-coeffect #(-> % (rc/update h dissoc :input-value))
                                                             :post-render    (fn [_]
                                                                               (-> (rc/get-handle ctx :ui-teams-tab)
                                                                                   (rc/dispatch :scroll-to-bottom)))}))

                          :focus                        (fn [handle _ _] (-> handle (rc/get-element :input) (.focus)))

                          [:input :on-key-down]         (fn [h _ _ e]
                                                          (d/handle-key e {[:ENTER] (fn [_] (rc/dispatch h :create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                                                           [:UP]    (fn [_] (-> (:ctx h)
                                                                                                (rc/get-handle :ui-teams-tab)
                                                                                                (rc/dispatch :focus-last-team)))}))

                          [:input :delete-on-backspace] (fn [h _ _ _] (when-let [{:keys [team-name team-id] :as t} (ui-services/last-team h)]
                                                                        (when (string/blank? team-name)
                                                                          (ui-services/dispatch-event
                                                                            {:event-type  [:team :delete]
                                                                             :ctx         (assoc (:ctx h) :team-id team-id)
                                                                             :post-render (fn [_]
                                                                                            (-> (:ctx h)
                                                                                                (rc/get-handle :ui-teams-tab)
                                                                                                (rc/dispatch :scroll-to-bottom)))}))))
                          [:button :on-click]           (fn [h _ _ _]
                                                          (rc/dispatch h :create-team)
                                                          (rc/dispatch h :focus))

                          [:button :on-key-down]        (fn [h _ _ e]
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