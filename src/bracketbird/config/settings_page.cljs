(ns bracketbird.config.settings-page
  (:require [bracketbird.state :as state]
            [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.util :as ut]))



(def settings-page {:config-name                   :settings-page
                    :ctx                           [:application-id :tournament-id]
                    :foreign-state                 (fn [ctx] (state/path-map ctx :hook/stages-order))

                    [:render]                      (fn [_]
                                                     (let [{:keys [hook/stages-order]} (rc/fs)]
                                                       [::tab-content
                                                        [::scroll-panel {:events [:scroll]}
                                                         [::tournament-start-row
                                                          [::start-icons]
                                                          [::start-stage
                                                           [::start-point]
                                                           [::start-line {:style (rs/style :stage-line)}]
                                                           [::hook-end {:style (rs/style :stage-hook-end)}]]]

                                                         (if (seq stages-order)
                                                           [::stages
                                                            (map (fn [stage-id]
                                                                   ^{:key stage-id} [rc/container {:stage-id stage-id} :stage-component]) stages-order)]

                                                           [::no-stages])
                                                         [::tournament-end-row
                                                          [::start-icons]
                                                          [::start-stage
                                                           [::start-point]
                                                           [::start-line {:style (rs/style :stage-line)}]
                                                           [::hook-end {:style (rs/style :stage-hook-end)}]]
                                                          ]]
                                                        [rc/container {} :add-stage-buttons]]))

                    [:tab-content :style]          (fn [_] (rs/style :tab-content {:scroll-top (rc/ls :scroll-panel-scroll-top)}))

                    [:scroll-panel :style]         (fn [_] (rs/style
                                                             (merge {:padding-top    [:layout-unit]
                                                                     :max-height     :100%
                                                                     :min-height     :200px
                                                                     :padding-bottom [:layout-unit]
                                                                     :overflow-y     :auto})))
                    [:tournament-start-row :style] (fn [_] (rs/style {:display    :flex
                                                                      :background :yellow}))
                    [:start-icons :style]          (fn [_] (rs/style {:width  [+ :app-padding :page-padding]
                                                                      :height :20 :background :blue}))

                    [:start-stage :style]          (fn [_] (rs/style {:width          [:stage-width]
                                                                      :display        :flex
                                                                      :flex-direction :column
                                                                      :align-items    :center
                                                                      :background     :orange}))

                    [:start-point :style]          (fn [_] (rs/style {:background    :red
                                                                      :width         20
                                                                      :border-radius 10
                                                                      :height        20
                                                                      }))

                    [:tournament-end :style]       (fn [_] (rs/style {:padding-left [+ :app-padding :page-padding]
                                                                      :height       40}))

                    [:no-stages :style]            (fn [_]
                                                     (rs/style {:height 80}))
                    })



(def stage-component {:config-name    :stage-component
                      :ctx            [:application-id :tournament-id :stage-id]
                      :foreign-state  (fn [ctx] (state/path-map ctx :hook/stage))

                      [:render]       (fn [_]
                                        [:div]
                                        #_(let [stage (rc/fs :hook/stage)]
                                            [::content
                                             [:upper-row
                                              [::icons {:events [:hover :click]}
                                               [ut/icon (rc/bind-options {:id :delete-icon :events [:click]}) "clear"]
                                               [::state-box]
                                               ]
                                              ]
                                             [:lower-row]
                                             [::icons]
                                             (str (get-in stage [:settings :settings-type]))
                                             ]))


                      [:icons :style] (fn [_] (rs/style {:padding-left [+ :app-padding :page-padding]
                                                         :background   :red
                                                         :height       :40
                                                         }))})


(def add-stage-buttons {:config-name              :add-stage-buttons
                        :ctx                      [:application-id :tournament-id]

                        [:render]                 (fn [_]
                                                    [::row
                                                     ;[:e/icon {[:button :hover?]} "wat"]
                                                     [::add-button :e/button "click me"]
                                                     [::add-knockout {:events [:key :click :hover]} "Add Knockout"]])


                        [:row :style]             (fn [_] (rs/style
                                                            {:padding-left [+ :app-padding :page-padding]
                                                             :display      :flex
                                                             :min-height   [:app-padding]
                                                             :align-items  :center}))

                        [:add-button :style]      (fn [_]
                                                    (rs/style :button {:active? (rc/ls :add-group-active?)
                                                                       :hover?  (rc/ls :add-group-hover?)}))

                        [:add-button :on-click]   (fn [_]
                                                    (rc/call 'create-stage :group))

                        [:add-knockout :style]    (fn [_]
                                                    (rs/style :button {:active? (rc/ls :add-knockout-active?)
                                                                       :hover?  (rc/ls :add-knockout-hover?)} :margin-left 20))

                        [:add-knockout :on-click] (fn [_]
                                                    (rc/put! :gensym (gensym))
                                                    #_(rc/dispatch h 'create-stage :knockout))



                        'create-stage             (fn [stage-type]
                                                    (ui-services/dispatch-event
                                                      {:event-type [:stage :create]
                                                       :ctx        (rc/this :ctx)
                                                       :content    {:stage-type stage-type}}))})
