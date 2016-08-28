(ns bracketbird.ui.frontpage
  (:require [bracketbird.app-state :as app-state]
            [ajax.core :refer [GET POST]]))


(defn- click []
  (GET "http://localhost:8080/bracketbird" {:handler       (fn [v] (println (type (cljs.reader/read-string (str v)))))
                 :error-handler (fn [e] (println (str "error" e)))}))

(defn logo []
  [:div {:style {:letter-spacing 0.8 :font-size 22}}
   [:span {:style {:color "lightblue"}} "BRACKET"]
   [:span {:style {:color "#C9C9C9"}} "BIRD"]])

(defn render [app-data]
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
              :on-click (fn [_] (click))} "Create a tournament"]
    [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])