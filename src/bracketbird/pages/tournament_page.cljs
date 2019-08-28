(ns bracketbird.pages.tournament-page
  (:require [bracketbird.pages.teams-tab :as teams]
            [bracketbird.pages.settings-tab :as settings]
            [bracketbird.pages.ranking-tab :as scores]
            [bracketbird.pages.matches-tab :as matches]
            [bracketbird.ui.styles :as s]
            [bracketbird.ui :as ui]
            [bracketbird.state :as state]))


(defn menu-panel [{:keys [on-selection selected order items]}]
  [:div {:style s/menu-panel-style}
   (map (fn [k]
          (let [selected? (= selected k)]
            ^{:key k} [:span {:on-click #(on-selection k)
                              :style    (merge s/menu-item-style (when selected? {:opacity 1 :cursor :auto}))}
                       (get-in items [k :header])])) order)])

(defn render [_ ui-path]
  (let [initial-values {:items             {:teams    {:header "TEAMS" :content teams/render}
                                            :settings {:header "SETTINGS" :content settings/render}
                                            :matches  {:header "MATCHES" :content matches/render}
                                            :ranking  {:header "SCORES" :content scores/render}}

                        :order             [:teams :settings :matches :ranking]
                        :selection-type    :single
                        :selected          :teams
                        :previous-selected :teams}

        values (state/subscribe-ui-values ui-path initial-values)
        on-selection (fn [tab] (state/update-ui-values! ui-path (ui/select @values tab)))]

    (fn [ctx ui-path]
      (println "render tournament page ")
      [:div {:style s/tournament-page-style}
       [menu-panel (assoc @values :on-selection on-selection)]
       ;content
       (let [selected (:selected @values)]
         (println "selected " (:selected @values))
         [(get-in @values [:items selected :content]) ctx (conj ui-path selected)])])))