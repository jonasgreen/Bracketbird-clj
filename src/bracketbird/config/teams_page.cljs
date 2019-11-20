(ns bracketbird.config.teams-page
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.state :as state]
            [bracketbird.util :as but]))


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
                                                      (println "update-team" team-id team-name)
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
                                                        :state-coeffect #(-> %
                                                                             (rc/update! add-team-handle dissoc :team-name-value))
                                                        :post-render    (fn [_]
                                                                          (rc/in this rc/call 'scroll-to-bottom)
                                                                          (rc/in this rc/focus :add-team :team-name))})))}
                 [:table]          {:decorate [:scroll]
                                    :style    #(rs/style :teams-page-table {:border-bottom? (< 0 (rc/ls :table-scroll-bottom))})}

                 'scroll-to-bottom #(but/scroll-to-bottom! :table)

                 'focus-last-team  (fn []
                                     (when (seq (rc/fs :hook/teams-order))
                                       (-> (merge (rc/this :ctx) {:team-id (last (rc/fs :hook/teams-order))})
                                           (rc/container-handle :team-row)
                                           (rc/dispatch 'focus))))})


