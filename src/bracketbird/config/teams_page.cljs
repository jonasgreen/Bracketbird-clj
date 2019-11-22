(ns bracketbird.config.teams-page
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.state :as state]
            [bracketbird.util :as but]
            [bracketbird.dom :as d]))


(def team-row {:config-name    :team-row
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
                                'action   #(rc/call 'delete-team (rc/fs [:hook/team :team-id]))}

               [:delete-icon]  {:style  #(rs/style :team-row-delete-icon {:visible? (rc/ls :row-hover?)
                                                                          :hover?   (rc/ls :icons-hover?)})
                                'action #(rc/call 'delete-team (rc/fs [:hook/team :team-id]))}

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

               'focus          #(rc/focus :team-name)
               'create-team-at (fn [team-id] (println "please implement 'create-team-at" team-id))
               'update-team    (fn [team-id team-name] (println "please implement 'update-team" team-id team-name))
               'delete-team    (fn [team-id] (println "please implement 'delete-team" team-id))
               'focus-up       (fn [from-team-id] (println "please implement 'focus-up" from-team-id))
               'focus-down     (fn [from-team-id] (println "please implement 'focus-down" from-team-id))})



(def add-team {:config-name   :add-team
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

               'create-team   (fn [team-name] (println "please implement 'create-team" team-name))})

(def teams-page {:config-name      :teams-page
                 :ctx              [:application-id :tournament-id]
                 :foreign-state    (fn [{:keys [rc-ctx]}] (state/path-map rc-ctx :hook/teams-order :hook/teams))

                 [:render]         (fn [_]
                                     (let [{:keys [hook/teams-order hook/teams]} (rc/fs)]
                                       [::content
                                        [::table
                                         (map (fn [team-id _]
                                                ^{:team-id team-id} [::team-row :c/team-row]) teams-order (range (count teams)))]

                                        ; input field
                                        [::add-team :c/add-team]]))


                 [:content]        {:style #(rs/style :tab-content {:scroll-top (rc/ls :table-scroll-top)})}

                 [:team-row]       {'focus-up       (fn [team-id-from]
                                                      (when-let [previous-team-id (-> (ui-services/previous-team team-id-from) :team-id)]
                                                        (rc/focus :team-row previous-team-id :team-name)))

                                    'focus-down     (fn [team-id-from]
                                                      (if-let [team-id (-> (ui-services/next-team team-id-from) :team-id)]
                                                        (rc/focus :team-row team-id :team-name)
                                                        (rc/focus :add-team :team-name)))

                                    'create-team-at (fn [team-id]
                                                      (let [{:keys [ctx] :as this} (rc/this)]
                                                        (ui-services/dispatch-event
                                                          {:event-type  [:team :create]
                                                           :ctx         ctx
                                                           :content     {:team-name ""
                                                                         :index     (ui-services/index-of team-id)}
                                                           :post-render (fn [{:keys [team-id]}]
                                                                          (rc/in this rc/focus :team-row team-id :team-name))})))


                                    'delete-team    (fn [team-id]
                                                      (let [{:keys [ctx] :as this} (rc/this)
                                                            team-row-handle (rc/get-handle :team-row team-id)
                                                            team-to-focus-after (-> (or
                                                                                      (ui-services/next-team team-id)
                                                                                      (ui-services/previous-team team-id))
                                                                                    :team-id)]

                                                        (ui-services/dispatch-event
                                                          {:event-type     [:team :delete]
                                                           :ctx            (assoc ctx :team-id team-id)
                                                           :state-coeffect #(-> % (rc/remove! team-row-handle))
                                                           :post-render    (fn [_]
                                                                             (if team-to-focus-after
                                                                               (rc/in this rc/focus :team-row team-to-focus-after :team-name)
                                                                               (rc/in this rc/focus :add-team :team-name)))})))

                                    'update-team    (fn [team-id team-name]
                                                      #_(when (rc/has-changed (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                                          (ui-services/dispatch-event
                                                            {:event-type [:team :update]
                                                             :ctx        (rc/this :ctx)
                                                             :content    {:team-name (rc/ls :team-name-value)}})))

                                    }
                 [:add-team]       {'create-team (fn [team-name]
                                                   (let [{:keys [ctx] :as this} (rc/this)
                                                         add-team-handle (rc/get-handle :add-team)]

                                                     (ui-services/dispatch-event
                                                       {:event-type     [:team :create]
                                                        :ctx            ctx
                                                        :content        {:team-name team-name}
                                                        :state-coeffect #(-> % (rc/update! add-team-handle dissoc :team-name-value))
                                                        :post-render    (fn [_]
                                                                          (rc/in this rc/call 'scroll-to-bottom)
                                                                          (rc/in this rc/focus :add-team :team-name))})))}
                 [:table]          {:decorate [:scroll]
                                    :style    #(rs/style :teams-page-table {:border-bottom? (< 0 (rc/ls :table-scroll-bottom))})}

                 'scroll-to-bottom #(but/scroll-to-bottom! :table)

                 'focus-last-team  (fn []
                                     #_(when (seq (rc/fs :hook/teams-order))
                                       (-> (merge (rc/this :ctx) {:team-id (last (rc/fs :hook/teams-order))})
                                           (rc/container-handle :team-row)
                                           (rc/dispatch 'focus))))})


