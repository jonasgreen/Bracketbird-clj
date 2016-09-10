(ns bracketbird.pages.tournament-page
  (:require [bracketbird.tournament-controller :as t-ctrl]
            [bracketbird.context :as context]
            [bracketbird.pages.teams :as teams]
            [bracketbird.pages.settings :as settings]
            [bracketbird.pages.scores :as scores]
            [bracketbird.pages.matches :as matches]
            [bracketbird.ui-selector :as s]

            [reagent.core :as r]))

(def m-teams {:name "Teams" :render teams/render})
(def m-settings {:name "Settings" :render settings/render})
(def m-matches {:name "Matches" :render matches/render})
(def m-scores {:name "Scores" :render scores/render})

(defn- menu-item [item selected item-selector-fn]
  [:span {:on-click (fn [e] (when-not selected (item-selector-fn item)))
          :style    {:padding 10 :cursor :pointer :font-weight (if selected :bold :normal)}} (:name item)])

(defn menu-panel [items selector]
  (r/create-class
    {:reagent-render
     (fn [items selector]
       [:div {:style {:background :yellow :height 40}}
        (doall (map (fn [item]
                      ^{:key (:name item)}
                      [menu-item item (= item (s/selected-item selector)) (s/item-selector selector)]) items))])

     :component-did-mount
     (fn [_] (s/initial-item-selector selector items))}))

(defn render [ctx]
  (let [selector (s/subscribe-single-item-selector ctx)]
    (fn [ctx]
      [:div
       [menu-panel (list m-teams m-settings m-matches m-scores) selector]
       ;content
       [:div
        (when-let [s-item (s/selected-item selector)]
          ((:render s-item) ctx))]
       ])))
