(ns bracketbird.config.application
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [stateless.util :as ut]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.system :as system]
            [bracketbird.state :as state]))



(def root {:config-name   :root
           :foreign-state (fn [ctx] (state/path-map ctx :hook/system))

           [:render]      (fn [_]
                            (let [app-id (rc/fs [:hook/system :active-application])]
                              (if app-id
                                ^{:application-id app-id} [rc/container :application-page]
                                [:div "No application"])))})

(def application-page {:config-name   :application-page
                       :ctx           [:application-id]
                       :local-state   (fn [_] {:active-page :front-page})
                       :foreign-state (fn [ctx] (state/path-map ctx :hook/application))

                       [:render]      (fn [data]
                                        (condp = (rc/ls :active-page)
                                          :front-page ^{:key :front-page} [rc/container :front-page]

                                          :tournament-page (let [tournament-id (-> (rc/fs [:hook/application :tournaments]) keys first)]
                                                             ^{:tournament-id tournament-id} [rc/container :tournament-page])

                                          [:div "page " (rc/ls :active-page) " not supported"]))})


(def front-page {:config-name                :front-page
                 :ctx                        [:application-id]

                 [:render]                   (fn [_]
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
                                                 [::create-tournament-button :e/large-button "Create a tournament"]
                                                 [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])

                 [:create-tournament-button] {'action (fn []
                                                        (let [{:keys [ctx]} (rc/this)
                                                              tournament-id (system/unique-id :tournament)]
                                                          (ui-services/dispatch-event
                                                            {:event-type     [:tournament :create]
                                                             :ctx            (assoc ctx :tournament-id tournament-id)
                                                             :content        {:tournament-id tournament-id}
                                                             :state-coeffect #(-> (rc/update! % (rc/container-handle ctx :application-page)
                                                                                              assoc
                                                                                              :active-page
                                                                                              :tournament-page))
                                                             :post-render    (fn [_])})))}})

(def tournament-page {:config-name      :tournament-page
                      :ctx              [:application-id :tournament-id]
                      :local-state      (fn [_] {:items             [{:id :teams :header "TEAMS" :content :teams-page}
                                                                     ;{:id :settings :header "SETTINGS" :content :settings-page}
                                                                     ;{:id :matches  :header "MATCHES" :content :matches-page}
                                                                     {:id :ranking :header "SCORES" :content :ranking-page}]

                                                 :order             [:teams :settings :matches :ranking]
                                                 :selection-type    :single
                                                 :selected          :teams
                                                 :previous-selected :teams})

                      [:render]         (fn [data]
                                          (let [{:keys [items order]} (rc/ls)
                                                tabs (sort-by #(ut/index-of (:id %) order) items)]

                                            [::page
                                             [::menu (map (fn [t] ^{:key (:id t)} [::menu-item (:header t)]) tabs)]
                                             (map (fn [t] ^{:key (:id t)} [::content-holder [rc/container (:content t)]]) tabs)]))


                      [:page]           {:style #(rs/style {:height         "100vh"
                                                            :display        :flex
                                                            :flex-direction :column})}

                      [:menu]           {:style (fn [_] (rs/style
                                                          {:font-size      22
                                                           :display        :flex
                                                           :align-items    :center
                                                           :min-height     [:app-padding]
                                                           :padding-left   [:app-padding]
                                                           :letter-spacing 1.2
                                                           :padding-right  [:app-padding]}))}

                      [:menu-item]      {:style    (fn [{:keys [rc-key]}]
                                                     (rs/style
                                                       (merge
                                                         {:margin-right [:layout-unit]
                                                          :opacity      0.5
                                                          :cursor       :pointer}
                                                         (when (= (rc/ls :selected) rc-key)
                                                           {:opacity 1 :cursor :auto}))))

                                         :on-click (fn [{:keys [rc-key]}]
                                                     (rc/call 'select-item rc-key))}

                      [:content-holder] {:style (fn [{:keys [rc-key] :as data}]
                                                  (rs/style (merge
                                                              {:height :100%}
                                                              (when-not (= (rc/ls :selected) rc-key)
                                                                {:display :none}))))}

                      'select-item      (fn [select]
                                          (rc/put! :previous-selected (rc/ls :selected)
                                                   :selected select))})
