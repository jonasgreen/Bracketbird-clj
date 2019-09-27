(ns bracketbird.pages
  (:require [bracketbird.styles :as s]
            [recontain.core :as rc]))

(defn ui-root [handle _ {:keys [hook/system]}]
  (let [ctx (:ctx handle)
        app-id (:active-application system)]

    [:div {:class :system}
     (if app-id
       [rc/container handle {:application-id app-id} :ui-application-page]
       [:div "No application"])]))

(defn ui-application-page [handle ls fs]
  (let [{:keys [ctx]} handle
        {:keys [active-page]} ls
        {:keys [hook/application]} fs]

    [:div {:class :application}
     (condp = active-page
       :ui-front-page ^{:key 1} [rc/container handle {} :ui-front-page]
       :ui-tournament-page ^{:key 2} (let [tournament-id (-> application :tournaments keys first)]
                                       [rc/container handle {:tournament-id tournament-id} :ui-tournament-page])
       [:div "page " active-page " not supported"])]))


(defn ui-front-page [handle _ _]

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
              :on-click (fn [_] (rc/dispatch handle :create-tournament))}

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
             ^{:key k} [:span {:on-click (fn [] (rc/dispatch handle :select-item k))
                               :style    (merge s/menu-item-style (when selected? {:opacity 1 :cursor :auto}))}
                        (get-in items [k :header])])) order)]

   ;content - show and hide by css display. If slow only mount elements that has been shown (to gain initially loading speed).
   (->> items
        (reduce-kv (fn [m k {:keys [content]}]
                     (conj m ^{:key k} [:div {:style (merge {:height :100%} (when-not (= selected k) {:display :none}))}
                                        [rc/container handle {} content]]))
                   [])
        seq)])