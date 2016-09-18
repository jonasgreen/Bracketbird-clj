(ns bracketbird.pages.tournament-page
  (:require [bracketbird.pages.teams-tab :as teams]
            [bracketbird.pages.settings-tab :as settings]
            [bracketbird.pages.scores-tab :as scores]
            [bracketbird.pages.matches-tab :as matches]
            [bracketbird.ui.ui-selector :as sel]
            [bracketbird.ui.styles :as s]
            [bracketbird.context :as context]
            [reagent.core :as r]
            [bracketbird.ui.panels :as p]))

;------------
; menu-items
;------------

(def m-teams {:name "TEAMS" :render (fn [ctx] [teams/render ctx])})
(def m-settings {:name "SETTINGS" :render (fn [ctx] [settings/render ctx])})
(def m-matches {:name "MATCHES" :render (fn [ctx] [matches/render ctx])})
(def m-scores {:name "SCORES" :render (fn [ctx] [scores/render ctx])})

(def m-items [m-teams m-settings m-matches m-scores])

(defn- menu-item [item selected select]
  [:span {:on-click (fn [e] (when-not selected (select item)))
          :style    (merge s/menu-item-style (when selected {:opacity 1 :cursor :auto}))}
   (:name item)])

;------------
; menu-panel
;------------

(defn menu-panel [items selector]
  (r/create-class

    {:reagent-render
     (fn [items selector]
       (let [selected-name (:name (sel/selected selector))
             item-selector (sel/item-selector selector)]
         [:div {:style s/menu-panel-style}
          (map (fn [item] ^{:key (:name item)} [menu-item item (= (:name item) selected-name) item-selector]) items)]))

     :component-did-mount
     (fn [_]
       (sel/initial-select selector items))}))

;--------------
; page-render
;--------------

(defn render [ctx]
  (let [selector (sel/subscribe-single-selection ctx)]
    (fn [ctx]
      [:div {:style s/tournament-page-style}
       [menu-panel m-items selector]
       ;content
       (when-let [{:keys [render name]} (sel/selected selector)]
         [render (context/sub-ui-ctx ctx [(keyword (str (clojure.string/lower-case name) "-tab"))])])])))