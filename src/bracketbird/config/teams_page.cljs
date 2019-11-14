(ns bracketbird.config.teams-page
  (:require [recontain.core :as rc]
            [restyle.core :as rs]

            [bracketbird.ui-services :as ui-services]
            [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bracketbird.dom :as d]))


(def teams-page {:config-name      :teams-page
                 :ctx              [:application-id :tournament-id]
                 :foreign-state    (fn [ctx] (state/path-map ctx :hook/teams-order :hook/teams))

                 [:render]         (fn [_]
                                     (let [{:keys [hook/teams-order hook/teams]} (rc/fs)]
                                       [::tab-content
                                        [::table
                                         (map (fn [team-id]

                                                ;;components seem to be way faster than containers
                                                ; ^{:key team-id} [::row ::default-input]) teams-order (range (count teams)))]

                                         ^{:key team-id} [rc/container {:team-id team-id} :team-row]) teams-order (range (count teams)))]

                                        ; input field
                                        [::bottom-panel
                                         [rc/container {} :add-team]]]))

                 [:tab-content]    {:style #(rs/style :tab-content {:scroll-top (rc/ls :table-scroll-top)})}

                 [:table]          {:decorate [:scroll]
                                    :style    (fn [_] (rs/style
                                                        (merge {:padding-top    [:layout-unit]
                                                                :max-height     :100%
                                                                :min-height     :200px
                                                                :padding-bottom [:layout-unit]
                                                                :overflow-y     :auto}
                                                               (when (< 0 (rc/ls :table-scroll-bottom)) {:border-bottom [:border]}))))}

                 'scroll-to-bottom (fn [] (-> (rc/this)
                                              (rc/dom-element :table)
                                              (ut/scroll-elm-to-bottom!)))

                 'focus-last-team  (fn []
                                     (when (seq (rc/fs :hook/teams-order))
                                       (-> (merge (rc/this :ctx) {:team-id (last (rc/fs :hook/teams-order))})
                                           (rc/container-handle :team-row)
                                           (rc/dispatch 'focus))))})


(def team-row {:config-name   :team-row
               :ctx           [:application-id :tournament-id :team-id]
               :foreign-state (fn [ctx] (state/path-map ctx :hook/team))
               :local-state   (fn [{:keys [hook/team]}]
                                {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})

               [:render]      (fn [data]
                                [::row
                                 [::icons]
                                  ;[::delete-icon ::icon "clear"]]

                                 [::space]
                                 [::seeding (inc (:rc-index data))]
                                 [::team-name ::default-input]])



               [:row]         {:decorate [:hover]
                               :style    (fn [data]
                                           (rs/style {:display     :flex
                                                            :align-items :center
                                                            :min-height  [:row-height]}))}

               [:icons]       {:style (fn [_] (rs/style
                                                {:display         :flex
                                                 :align-items     :center
                                                 :height          [:row-height]
                                                 :justify-content :center
                                                 :cursor          (if (rc/ls :icons-hover?) :pointer :normal)
                                                 :width           [:app-padding]}))}

               [:delete-icon] {:style (fn [_] (rs/style (merge {:font-size  8
                                                                :opacity    0.5
                                                                :transition "background 0.2s, color 0.2s, border-radius 0.2s"}
                                                               (when-not (rc/ls :row-hover?)
                                                                 {:color :transparent})

                                                               (when (rc/ls :icons-hover?)
                                                                 {:font-weight   :bold
                                                                  :background    :red
                                                                  :color         :white
                                                                  :font-size     10
                                                                  :border-radius 8}))))}

               [:space]       {:style #(rs/style {:width [:page-padding]})}

               [:seeding]     {:style #(rs/style {:display     :flex
                                                  :align-items :center
                                                  :width       [:seeding-width]
                                                  :opacity     0.5
                                                  :font-size   10})}


               [:team-name]   {[:input :style] #(rs/style
                                                  {:border     :none
                                                   :padding    0
                                                   :background :red
                                                   :min-width  200})
                               [:input :value] #(or (rc/ls :team-name :input-value)
                                                    (rc/fs [:hook/team :team-name]))
                               'action         #()}


               ;[:icons :on-click]                (fn [_] (rc/call 'delete-team))
               ;[:delete-icon :on-click]          (fn [_] (rc/call 'delete-team))
               #_[:team-name :on-key-down] #_(fn [_]
                                               (let [this (rc/this)]
                                                 (d/handle-key (rc/ls :event) {:ESC            (fn [_] (rc/delete-local-state this) [:STOP-PROPAGATION])
                                                                               :ENTER          (fn [_] (rc/dispatch this 'update-team))
                                                                               [:SHIFT :ENTER] (fn [_] (ui-services/dispatch-event
                                                                                                         {:event-type  [:team :create]
                                                                                                          :ctx         (:ctx this)
                                                                                                          :content     {:team-name ""
                                                                                                                        :index     (ui-services/index-of (:ctx this) (rc/fs [:hook/team :team-id]))}
                                                                                                          :post-render (fn [event]
                                                                                                                         (-> (:ctx this)
                                                                                                                             (assoc :team-id (:team-id event))
                                                                                                                             (rc/container-handle :team-row)
                                                                                                                             (rc/dispatch 'focus)))}))
                                                                               :UP             (fn [_] (->> (rc/fs [:hook/team :team-id])
                                                                                                            (ui-services/previous-team this)
                                                                                                            (rc/focus this :team-row :team-id)))
                                                                               :DOWN           (fn [_] (let [team-to-focus (ui-services/after-team this (rc/fs [:hook/team :team-id]))]
                                                                                                         (if team-to-focus
                                                                                                           (rc/focus this :team-row :team-id team-to-focus)
                                                                                                           (rc/focus this :add-team))))})))
               ;[:team-name :delete-on-backspace] (fn [_] (rc/call 'delete-team))
               ;[:team-name :on-blur]             (fn [_] (rc/call 'update-team))

               'update-team   (fn [_]
                                (when (rc/has-changed (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                  (ui-services/dispatch-event
                                    {:event-type [:team :update]
                                     :ctx        (rc/this :ctx)
                                     :content    {:team-name (rc/ls :team-name-value)}})))
               'delete-team   (fn [_]
                                (let [this (rc/this)
                                      team-id (rc/fs [:hook/team :team-id])
                                      team-to-focus (or
                                                      (ui-services/after-team this team-id)
                                                      (ui-services/previous-team this team-id))]
                                  (ui-services/dispatch-event
                                    {:event-type  [:team :delete]
                                     :ctx         (assoc (:ctx this) :team-id team-id)
                                     :post-render (fn [_]
                                                    (if team-to-focus
                                                      (rc/focus this :team-row :team-id team-to-focus)
                                                      (rc/focus this :add-team)))})))
               'focus         (fn [_] (-> (rc/this) (rc/dom-element :team-name) (.focus)))})

(def add-team {:config-name       :add-team
               :ctx               [:application-id :tournament-id]
               ;:local-state               (fn [_] {:input-delete-on-backspace? true})
               :foreign-state     (fn [ctx] (state/path-map ctx :hook/teams))

               [:render]          (fn [_]
                                    [::row
                                     [::team-name ::default-input]
                                     [::add-team-button ::primary-button {:text "Add team"}]])


               [:row]             {:style (fn [_]
                                            (rs/style
                                              {:padding-left [+ :app-padding :page-padding (when (seq (rc/fs :hook/teams)) :seeding-width)]
                                               :display      :flex
                                               :min-height   [:app-padding]
                                               :align-items  :center}))}


               [:team-name]       {[:input :placeholder] (fn [_] "Enter team")
                                   'action               (fn [] (rc/call 'create-team))}

               ;[:input :on-change]    (fn [this]
               ;                         (println "onchange" (.. (rc/ls :event) -target -value))
               ;                         (rc/put! this assoc :input-value (.. (rc/ls :event) -target -value)))

               #_[:input :on-key-down]  #_(fn [this]
                                            (d/handle-key (rc/ls :event) {[:ENTER] (fn [_] (rc/dispatch this :'create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                                                          [:UP]    (fn [_] (-> this
                                                                                               :ctx
                                                                                               (rc/container-handle :teams-page)
                                                                                               (rc/dispatch 'focus-last-team)))}))
               #_[:input :delete-on-backspace] #_(fn [{:keys [ctx]} _ _]
                                                   (when-let [{:keys [team-name team-id]} (ui-services/last-team ctx)]
                                                     (when (string/blank? team-name)
                                                       (ui-services/dispatch-event
                                                         {:event-type  [:team :delete]
                                                          :ctx         (assoc ctx :team-id team-id)
                                                          :post-render (fn [_]
                                                                         (-> ctx
                                                                             (rc/container-handle :teams-page)
                                                                             (rc/dispatch 'scroll-to-bottom)))}))))

               [:add-team-button] {'action (fn []
                                             (rc/call 'create-team)
                                             (rc/call 'focus))}


               'create-team       (fn []
                                    (let [{:keys [ctx] :as this} (rc/this)
                                          team-name-handle (rc/component-handle :team-name)
                                          team-name-value (rc/ls :team-name :input-value)]

                                      (ui-services/dispatch-event
                                        {:event-type     [:team :create]
                                         :ctx            ctx
                                         :content        {:team-name team-name-value}
                                         :state-coeffect #(-> % (rc/update! team-name-handle dissoc :input-value))
                                         :post-render    (fn [_]
                                                           (-> (rc/container-handle ctx :teams-page)
                                                               (rc/dispatch 'scroll-to-bottom)))})))
               'focus             (fn [] (-> (rc/component-handle :team-name)
                                             (rc/dispatch 'focus)))})