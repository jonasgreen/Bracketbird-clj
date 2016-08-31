(ns bracketbird.pages.front-page
  (:require [bracketbird.application-controller :as app-ctrl]))

(defn logo []
  [:div {:style {:letter-spacing 0.8 :font-size 22}}
   [:span {:style {:color "lightblue"}} "BRACKET"]
   [:span {:style {:color "#C9C9C9"}} "BIRD"]])

(defn render [ctx]
  (println "render-front-page")
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
              :on-click #(app-ctrl/create-tournament)} "Create a tournament"]
    [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])