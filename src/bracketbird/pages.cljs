(ns bracketbird.pages
  (:require [bracketbird.styles :as s]
            [bracketbird.hookit :as h]))


(defn ui-root [handle _ {:keys [hooks/system]}]
  (let [id (:active-application system)]
    [:div {:class :system}
     (if id
       [h/build handle :hooks/ui-application-page {:application-id id}]
       [:div "No application"])]))


(defn application-page [handle {:keys [active-page]} {:keys [hooks/application]}]
  [:div {:class :application} (condp = active-page
                                :hooks/ui-front-page ^{:key 1} [h/build handle :hooks/ui-front-page]
                                :hooks/ui-tournament-page ^{:key 2} [h/build handle :hooks/ui-tournament-page (-> (:tournament application)
                                                                                                                  (select-keys [:tournament-id]))]
                                [:div "page " (:active-page application) " not supported"])])


(defn front-page [handle _ _]
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
              :on-click (fn [_] (h/dispatch handle :create-tournament))}

     "Create a tournament"]
    [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])


(defn tournament-page [handle {:keys [selected order items]} _]
  ;page

  [:div {:style s/tournament-page-style}

   [:div {:style {:position :fixed :top 20 :right 200}}]
   ;menu
   [:div {:style s/menu-panel-style}
    (map (fn [k]
           (let [selected? (= selected k)]
             ^{:key k} [:span {:on-click (fn [] (h/dispatch handle :select-item k))
                               :style    (merge s/menu-item-style (when selected? {:opacity 1 :cursor :auto}))}
                        (get-in items [k :header])])) order)]

   ;content - show and hide by css display. If slow only mount elements that has been shown (to gain initially loading speed).
   (->> items
        (reduce-kv (fn [m k {:keys [content]}]
                     (conj m ^{:key k} [:div {:style (merge {:height :100%} (when-not (= selected k) {:display :none}))}
                                        [h/build handle content]]))
                   [])
        seq)])