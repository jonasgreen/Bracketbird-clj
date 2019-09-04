(ns bracketbird.pages
  (:require [bracketbird.ui :as ui]
            [bracketbird.state :as state]
            [bracketbird.styles :as s]
            [bracketbird.ui-services :as ui-services]))



(defn system [_ {:keys [hooks/system]}]
  (let [id (:active-application system)]
    [:div {:class :system}
     (if id
       [ui/gui :hooks/ui-application-page {:application-id id}]
       [:div "No application"])]))


(defn application [ctx {:keys [hooks/application]}]
  [:div {:class :application} (condp = (:active-page application)
                                :front-page ^{:key 1} [ui/gui :hooks/ui-front-page ctx]
                                :tournament-page ^{:key 2} [ui/gui :hooks/ui-tournament-page (-> (:tournament application)
                                                                                                 (select-keys [:tournament-id])
                                                                                                 (merge ctx))]
                                [:div "page " (:active-page application) " not supported"])])


(defn front [ctx opts]
  [:div
   [:div {:style {:display         :flex
                  :justify-content :center
                  :padding-top     30}}

    ;floating logo
    [:div {:style {:width 900}}
     [:div {:style {:letter-spacing 0.8 :font-size 22}}
      [:span {:style {:color "lightblue"}} "BRACKET"]
      [:span {:style {:color "#C9C9C9"}} "BIRD"]]]]

   [:div {:style {:display :flex :flex-direction :column :align-items :center}}
    [:div {:style {:font-size 48 :padding "140px 0 30px 0"}}
     "Instant tournaments"]
    [:button {:class    "largeButton primaryButton"
              :on-click (fn [_]
                          (let [app-path (state/hook-path :hooks/application ctx)
                                show-tournament-page (fn [state] (assoc-in state (conj app-path :active-page) :tournament-page))]
                            (ui-services/dispatch-event [:tournament :create] ctx {} {:state-coeffect show-tournament-page})))}

     "Create a tournament"]
    [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])


(defn tournament [ctx {:keys [values] :as opts}]
  (let [{:keys [selected order items]} values]
    ;page
    [:div {:style s/tournament-page-style}

     ;menu
     [:div {:style s/menu-panel-style}
      (map (fn [k]
             (let [selected? (= selected k)]
               ^{:key k} [:span {:on-click #(state/put! values :previous-selected (:selected values) :selected k)
                                 :style    (merge s/menu-item-style (when selected? {:opacity 1 :cursor :auto}))}
                          (get-in items [k :header])])) order)]

     ;content - show and hide by css display. If slow only mount elements that has been shown (to gain initially loading speed).
     (->> items
          (reduce-kv (fn [m k {:keys [content]}]
                       (conj m ^{:key k} [:div {:style (merge {:height :100%} (when-not (= selected k) {:display :none}))}
                                          [ui/gui content ctx]]))
                     [])
          seq)]))