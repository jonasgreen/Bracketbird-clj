(ns bracketbird.pages.front-page
  (:require [bracketbird.ui-services :as ui-services]
            [bracketbird.state :as state]))

(defn logo []
  [:div {:style {:letter-spacing 0.8 :font-size 22}}
   [:span {:style {:color "lightblue"}} "BRACKET"]
   [:span {:style {:color "#C9C9C9"}} "BIRD"]])

(defn render [ctx ui-path]
  [:div
   [:div {:style {:display         :flex
                  :justify-content :center
                  :padding-top     30}}

    ;floating logo
    [:div {:style {:width 900}}
     [logo]]]

   [:div {:style {:display :flex :flex-direction :column :align-items :center}}
    [:div {:style {:font-size 48 :padding "140px 0 30px 0"}}
     "Instant tournaments"]
    [:button {:class    "largeButton primaryButton"
              :on-click (fn [_]
                          (let [app-path (state/mk-path :application ctx)
                                show-tournament-page (fn [state] (assoc-in state (conj app-path :active-page) :tournament-page))]
                            (ui-services/dispatch-event [:tournament :create] ctx {} {:state-coeffect show-tournament-page})))}

     "Create a tournament"]
    [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])