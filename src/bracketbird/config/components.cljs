(ns bracketbird.config.components
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.dom :as d]
            [bracketbird.state :as state]
            [bracketbird.ui-services :as ui-services]))



(def elements {:icon         {:render [:i]
                              :style  (rs/style :icon)}

               :button       {:render      [:div]
                              :decorate    [:hover :active :action]
                              :style       (fn [{:keys [rc-button-style] :as d}]
                                             (rs/style (if rc-button-style rc-button-style :button)
                                                       {:active? (rc/ls (rc/sub-name d :active?))
                                                        :hover?  (rc/ls (rc/sub-name d :hover?))}))

                              'action      (fn [_] (println "button 'action please implement... "))}

               :large-button {:inherits :button
                              :style    #(rc/super :style (assoc % :rc-button-style :large-button))}



               :input        {:render      [:input]
                              :decorate    [:hover :change :focus :key-enter-action]
                              :style       {:border :none :padding 0}
                              :type        :text
                              :placeholder "Type some text"

                              'action      (fn [] (println "input 'action please implement "))}})


(def components {:team-row {:config-name    :team-row
                            :ctx            [:application-id :tournament-id :team-id]

                            :foreign-state  (fn [{:keys [rc-ctx]}] (state/path-map rc-ctx :hook/team))
                            :local-state    (fn [{:keys [hook/team]}] {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})

                            [:render]       (fn [data]
                                              [::row
                                               [::icons
                                                [::delete-icon :e/icon "clear"]]
                                               [::space]
                                               [::seeding (inc (:rc-index data))]
                                               [::team-name :e/input]])

                            [:row]          {:decorate [:hover] :style #(rs/style :team-row)}

                            [:icons]        {:decorate [:hover]
                                             :style    #(rs/style :team-row-icons {:hover? (rc/ls :icons-hover?)})
                                             :on-click #(rc/call 'delete-team (rc/fs [:hook/team :team-id]))}

                            [:delete-icon]  {:style    #(rs/style :team-row-delete-icon {:visible? (rc/ls :row-hover?)
                                                                                         :hover?   (rc/ls :icons-hover?)})
                                             :on-click #(rc/call 'delete-team (rc/fs [:hook/team :team-id]))}

                            [:space]        {:style #(rs/style :team-row-space)}
                            [:seeding]      {:style #(rs/style :team-row-seeding)}

                            [:team-name]    {:style       #(rs/style :team-row-team-name)
                                             :value       #(or (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                             :on-key-down (fn [{:keys [rc-event] :as data}]
                                                            (rc/super :on-key-down data)
                                                            (let [team-id (rc/fs [:hook/team :team-id])]
                                                              (d/handle-key rc-event {:ESC            (fn [_] (rc/delete-local-state) [:STOP-PROPAGATION])
                                                                                      [:SHIFT :ENTER] (fn [_] (rc/call 'create-team-at team-id))
                                                                                      :UP             (fn [_] (rc/call 'focus-up team-id))
                                                                                      :DOWN           (fn [_] (rc/call 'focus-down team-id))})))

                                             'action      (fn [] (rc/call 'update-team (rc/fs [:hook/team :team-id]) (rc/ls :team-name-value)))}

                            'focus          (fn [_] (-> (rc/this) (rc/dom-element :team-name) (.focus)))

                            'create-team-at (fn [team-id] (println "please implement 'create-team-at" team-id))
                            'update-team    (fn [team-id team-name] (println "please implement 'update-team" team-id team-name))
                            'delete-team    (fn [team-id] (println "please implement 'delete-team" team-id))
                            'focus-up       (fn [from-team-id] (println "please implement 'focus-up" from-team-id))
                            'focus-down     (fn [from-team-id] (println "please implement 'focus-down" from-team-id))}

                 ;-----

                 :add-team {:config-name   :add-team
                            :ctx           [:application-id :tournament-id]
                            ;:local-state               (fn [_] {:input-delete-on-backspace? true})
                            :foreign-state (fn [{:keys [rc-ctx]}] (state/path-map rc-ctx :hook/teams))

                            [:render]      (fn [_]
                                             [::row
                                              [::team-name :e/input]
                                              [::add-button :e/button "Add team"]])

                            [:row]         {:style #(rs/style :add-team-row {:extra-padding? (seq (rc/fs :hook/teams))})}

                            [:team-name]   {:placeholder (fn [_] "Enter team")
                                            'action      #(rc/call 'create-team (rc/ls :team-name-value))}

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

                            [:add-button]  {'action #(rc/call 'create-team (rc/ls :team-name-value))}

                            'create-team   (fn [team-name] (println "please implement 'create-team" team-name))}})