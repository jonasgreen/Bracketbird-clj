(ns bracketbird.config.application
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.system :as system]
            [bracketbird.state :as state]))



(def root {:hook          :root
           :foreign-state (fn [ctx]
                            (state/path-map ctx :hook/system))

           :render        (fn [_]
                            (let [app-id (rc/fs [:hook/system :active-application])]
                              [:div
                               (if app-id
                                 [rc/container {:application-id app-id} :application-page]
                                 [:div "No application"])]))})

(def application-page {:hook          :application-page
                       :ctx           [:application-id]
                       :local-state   (fn [_] {:active-page :front-page})
                       :foreign-state (fn [ctx] (state/path-map ctx :hook/application))

                       :render        (fn [_]
                                        (condp = (rc/ls :active-page)
                                          :front-page ^{:key 1} [rc/container {} :front-page]
                                          :tournament-page ^{:key 2} (let [tournament-id (-> (rc/fs [:hook/application :tournaments]) keys first)]
                                                                          [rc/container {:tournament-id tournament-id} :tournament-page])
                                          [:div "page " (rc/ls :active-page) " not supported"]))})


(def front-page {:hook              :front-page
                 :ctx               [:application-id]

                 :render            (fn [h]
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

                 :create-tournament (fn [h]
                                      (let [ctx (:ctx h)
                                            tournament-id (system/unique-id :tournament)]
                                        (ui-services/dispatch-event
                                          {:event-type     [:tournament :create]
                                           :ctx            (assoc ctx :tournament-id tournament-id)
                                           :content        {:tournament-id tournament-id}
                                           :state-coeffect #(-> (rc/update! % (rc/get-handle ctx :application-page)
                                                                            assoc
                                                                            :active-page
                                                                            :tournament-page))
                                           :post-render    (fn [_])})))})

(def tournament-page {:hook                    :tournament-page
                      :ctx                     [:application-id :tournament-id]
                      :local-state             (fn [_] {:items             {:teams    {:header "TEAMS" :content :teams-page}
                                                                            :settings {:header "SETTINGS" :content :settings-page}
                                                                            :matches  {:header "MATCHES" :content :matches-page}
                                                                            :ranking  {:header "SCORES" :content :ranking-page}}

                                                        :order             [:teams :settings :matches :ranking]
                                                        :selection-type    :single
                                                        :selected          :teams
                                                        :previous-selected :teams})

                      :render                  (fn [_]
                                                 (let [{:keys [items order]} (rc/ls)]
                                                   [::page
                                                    [::menu (map (fn [k] ^{:key k}
                                                                   [::menu-item {:current/item k
                                                                                 :events       [:click]} (get-in items [k :header])]) order)]
                                                    (->> items
                                                         (reduce-kv (fn [m k {:keys [content]}]
                                                                      (conj m ^{:key k} [::content-holder {:current/item k} [rc/container {} content]]))
                                                                    [])
                                                         seq)]))


                      [:page :style]           (fn [_] (rs/style
                                                         {:height         "100vh"
                                                          :display        :flex
                                                          :flex-direction :column}))

                      [:menu :style]           (fn [_] (rs/style
                                                         {:font-size      22
                                                          :display        :flex
                                                          :align-items    :center
                                                          :min-height     [:app-padding]
                                                          :padding-left   [:app-padding]
                                                          :letter-spacing 1.2
                                                          :padding-right  [:app-padding]}))

                      [:menu-item :style]      (fn [_] (rs/style
                                                         (merge
                                                           {:margin-right [:layout-unit]
                                                            :opacity      0.5
                                                            :cursor       :pointer}
                                                           (when (= (rc/ls :selected) (rc/ls :current/item)) {:opacity 1 :cursor :auto}))))

                      [:menu-item :on-click]   (fn [h _]
                                                 (rc/dispatch h :select-item (rc/ls :current/item)))


                      [:content-holder :style] (fn [_] (rs/style
                                                         (merge {:height :100%} (when-not (= (rc/ls :selected) (rc/ls :current/item))
                                                                                  {:display :none}))))

                      :select-item             (fn [h select]
                                                 (rc/put! h assoc :previous-selected (rc/ls :selected) :selected select))})
