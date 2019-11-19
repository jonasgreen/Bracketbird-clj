(ns bracketbird.config.teams-page
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.state :as state]
            [bracketbird.util :as ut]))


(def teams-page {:config-name      :teams-page
                 :ctx              [:application-id :tournament-id]
                 :foreign-state    (fn [ctx] (state/path-map ctx :hook/teams-order :hook/teams))

                 [:render]         (fn [_]
                                     (let [{:keys [hook/teams-order hook/teams]} (rc/fs)]
                                       [::content
                                        [::table
                                         (map (fn [team-id _]
                                                ^{:team-id team-id} [::team-row :c/team-row]) teams-order (range (count teams)))]

                                        ; input field
                                        [::add-team :c/add-team]]))


                 [:content]        {:style #(rs/style :tab-content {:scroll-top (rc/ls :table-scroll-top)})}

                 [:team-row]       {'focus-up       (fn [team-id-from] (println "up" team-id-from)
                                                      #_(->> (rc/fs [:hook/team :team-id])
                                                             (ui-services/previous-team this)
                                                             (rc/focus this :team-row :team-id))

                                                      )
                                    'focus-down     (fn [team-id-from]
                                                      (println "up" team-id-from)
                                                      #_(let [team-to-focus (ui-services/after-team this (rc/fs [:hook/team :team-id]))]
                                                          (if team-to-focus
                                                            (rc/focus this :team-row :team-id team-to-focus)
                                                            (rc/focus this :add-team)))
                                                      )

                                    'create-team-at (fn [team-id]
                                                      (println "create-team-at" team-id)
                                                      #_(ui-services/dispatch-event
                                                          {:event-type  [:team :create]
                                                           :ctx         (:ctx this)
                                                           :content     {:team-name ""
                                                                         :index     (ui-services/index-of (:ctx this) team-id)}
                                                           :post-render (fn [event]
                                                                          (-> (:ctx this)
                                                                              (assoc :team-id (:team-id event))
                                                                              (rc/container-handle :team-row)
                                                                              (rc/dispatch 'focus)))})

                                                      )


                                    'delete-team    (fn [team-id]
                                                      (println "delete team" team-id)
                                                      #_(let [this (rc/this)
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

                                    'update-team    (fn [team-id team-name]
                                                      (println "update-team" team-id team-name)
                                                      #_(when (rc/has-changed (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                                          (ui-services/dispatch-event
                                                            {:event-type [:team :update]
                                                             :ctx        (rc/this :ctx)
                                                             :content    {:team-name (rc/ls :team-name-value)}})))

                                    }
                 [:add-team]       {'create-team (fn [team-name]
                                                   (let [{:keys [ctx] :as this} (rc/this)
                                                         add-team-handle (-> this (rc/get-handle-id :add-team) rc/get-handle)]
                                                     (ui-services/dispatch-event
                                                       {:event-type     [:team :create]
                                                        :ctx            ctx
                                                        :content        {:team-name team-name}
                                                        :state-coeffect #(-> % (rc/update! add-team-handle dissoc :team-name-value))
                                                        :post-render    (fn [_]
                                                                          (rc/call-in this 'scroll-to-bottom)
                                                                          (println "-------")
                                                                          (-> this (rc/dom-element :add-team :team-name) (.focus))
                                                                          )})))}
                 [:table]          {:decorate [:scroll]
                                    :style    (fn [_] (rs/style
                                                        (merge {:padding-top    [:layout-unit]
                                                                :max-height     :100%
                                                                :min-height     :200px
                                                                :padding-bottom [:layout-unit]
                                                                :overflow-y     :auto}
                                                               (when (< 0 (rc/ls :table-scroll-bottom)) {:border-bottom [:border]}))))}

                 'scroll-to-bottom (fn []
                                     (-> (rc/this)
                                         (rc/dom-element :table)
                                         (ut/scroll-elm-to-bottom!)))

                 'focus-last-team  (fn []
                                     (when (seq (rc/fs :hook/teams-order))
                                       (-> (merge (rc/this :ctx) {:team-id (last (rc/fs :hook/teams-order))})
                                           (rc/container-handle :team-row)
                                           (rc/dispatch 'focus))))})


