(ns bracketbird.config.components
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.dom :as d]
            [bracketbird.state :as state]
            [bracketbird.ui-services :as ui-services]))



(def elements {:icon         {:render [:i]
                              :style  {:font-family             "Material Icons"
                                       :font-weight             "normal"
                                       :font-style              "normal"
                                       :font-size               8
                                       :display                 "inline-block"
                                       :width                   "1em"
                                       :height                  "1em"
                                       :opacity                 0.5
                                       :line-height             "1"
                                       :text-transform          "none"
                                       :letter-spacing          "normal"
                                       :word-wrap               "normal"
                                       ;Support for all WebKit browsers.
                                       :-webkit-font-smoothing  "antialiased"
                                       ;Support for Safari and Chrome.
                                       :text-rendering          "optimizeLegibility"
                                       ;Support for Firefox.
                                       :-moz-osx-font-smoothing "grayscale"
                                       ;Support for IE.
                                       :font-feature-settings   "liga"
                                       :transition              "background 0.2s, color 0.2s, border-radius 0.2s"}}

               :button       {:render      [:div]
                              :decorate    [:hover :active]
                              :style       (fn [{:keys [rc-button-style] :as d}]
                                             (rs/style (if rc-button-style rc-button-style :button)
                                                       {:active? (rc/ls (rc/sub-name d :active?))
                                                        :hover?  (rc/ls (rc/sub-name d :hover?))}))
                              :on-click    #(rc/call 'action)
                              :on-key-down (fn [{:keys [rc-event]}]
                                             (d/handle-key
                                               rc-event
                                               {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                              'action      (fn [_] (println "button 'action ... "))}

               :large-button {:inherits :button
                              :style    #(rc/super :style (assoc % :rc-button-style :large-button))}



               :input        {:render      [:input]
                              :decorate    [:hover :change :focus]
                              :style       {:border :none :padding 0}
                              :type        :text
                              :placeholder "Type som text"
                              :on-key-down (fn [{:keys [rc-event]}]
                                             (d/handle-key
                                               rc-event
                                               {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                              'focus       (fn [] (rc/focus-dom-element :input))
                              'action      (fn [] (println "input 'action ... " (rc/ls :input-value)))}})





(def components {:team-row {:config-name   :team-row
                            :ctx           [:application-id :tournament-id :team-id]

                            :foreign-state (fn [{:keys [rc-ctx]}] (state/get-data rc-ctx :hook/team))
                            :local-state   (fn [{:keys [hook/team]}]
                                             {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})



                            [:render]      (fn [data]
                                             [::row
                                              [::icons
                                               [::delete-icon :e/icon "clear"]]

                                              [::space]
                                              [::seeding (inc (:rc-index data))]
                                              [::team-name :e/input]
                                              ])

                            [:row]         {:decorate [:hover]
                                            :style    (fn [data]
                                                        (rs/style {:display     :flex
                                                                   :align-items :center
                                                                   :min-height  [:row-height]}))}

                            [:icons]       {:decorate [:hover]
                                            :style    (fn [_] (rs/style
                                                                {:display         :flex
                                                                 :align-items     :center
                                                                 :height          [:row-height]
                                                                 :justify-content :center
                                                                 :cursor          (if (rc/ls :icons-hover?) :pointer :normal)
                                                                 :width           [:app-padding]}))
                                            :on-click #(rc/call 'delete-team)}

                            [:delete-icon] {:style    #(merge (rc/super :style)
                                                              (when-not (rc/ls :row-hover?)
                                                                {:color :transparent})

                                                              (when (rc/ls :icons-hover?)
                                                                {:font-weight   :bold
                                                                 :background    :red
                                                                 :color         :white
                                                                 :font-size     10
                                                                 :border-radius 8}))
                                            :on-click #(rc/call 'delete-team)}

                            [:space]       {:style #(rs/style {:width [:page-padding]})}

                            [:seeding]     {:style #(rs/style {:display     :flex
                                                               :align-items :center
                                                               :width       [:seeding-width]
                                                               :opacity     0.5
                                                               :font-size   10})}


                            [:team-name]   {:style  #(rs/style
                                                       {:border    :none
                                                        :padding   0
                                                        :min-width 200})
                                            ;:value  #(or (rc/ls :team-name :input-value)
                                            ;             (rc/fs [:hook/team :team-name]))
                                            'action #()}



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
                                             (println "delete team")
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
                            'focus         (fn [_] (-> (rc/this) (rc/dom-element :team-name) (.focus)))}})

#_(def components {:primary-button {[:render] (fn [{:keys [text]}]
                                                [::button (or text "a primary button")])

                                    [:button] {:decorate    [:hover :active]
                                               :style       #(rs/style :primary-button {:active? (rc/ls :button-active?)
                                                                                        :hover?  (rc/ls :button-hover?)})
                                               :on-click    #(rc/call 'action)
                                               :on-key-down (fn [{:keys [rc-event]}]
                                                              (d/handle-key
                                                                rc-event
                                                                {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))}

                                    'action   (fn [_] (println "default-button 'action ... "))}


                   :default-input  {[:render] (fn [_] [::input :input])

                                    [:input]  {:decorate    [:hover :change :focus]
                                               :style       {:border :none :padding 0}
                                               :type        :text
                                               :placeholder "Type som text"
                                               :on-key-down (fn [{:keys [rc-event]}]
                                                              (d/handle-key
                                                                rc-event
                                                                {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))}

                                    'focus    (fn [] (rc/focus-dom-element :input))
                                    'action   (fn [] (println "default-input 'action ... " (rc/ls :input-value)))}
                   })




