(ns bracketbird.pages.tournament-page
  (:require [bracketbird.components.teams-tab :as teams]
            [bracketbird.components.settings-tab :as settings]
            [bracketbird.components.ranking-tab :as scores]
            [bracketbird.components.matches-tab :as matches]
            [bracketbird.styles :as s]
            [bracketbird.ui :as ui]
            [bracketbird.state :as state]))


(defn render [ctx]
  (let [initial-value {:items             {:teams    {:header "TEAMS" :content teams/render}
                                           :settings {:header "SETTINGS" :content settings/render}
                                           :matches  {:header "MATCHES" :content matches/render}
                                           :ranking  {:header "SCORES" :content scores/render}}

                       :order             [:teams :settings :matches :ranking]
                       :selection-type    :single
                       :selected          :teams
                       :previous-selected :teams}

        ui-values (state/hook :tournament-page ctx initial-value)
        on-selection (fn [tab] (-> @ui-values (ui/select tab) state/update-ui!))]

    (fn [ctx]
      (let [{:keys [selected order items]} @ui-values]
        ;page
        [:div {:style s/tournament-page-style}

         ;menu
         [:div {:style s/menu-panel-style}
          (map (fn [k]
                 (let [selected? (= selected k)]
                   ^{:key k} [:span {:on-click #(on-selection k)
                                     :style    (merge s/menu-item-style (when selected? {:opacity 1 :cursor :auto}))}
                              (get-in items [k :header])])) order)]

         ;content - show and hide by css display. If slow only mount elements that has been shown (to gain initially loading speed).
         (->> items
              (reduce-kv (fn [m k {:keys [content]}]
                           (conj m ^{:key k} [:div {:style (merge s/tournamet-tab-content-style (when-not (= selected k) {:display :none}))}
                                              [content ctx]]))
                         [])
              seq)]))))