(ns bracketbird.pages.tournament-page
  (:require [bracketbird.pages.teams :as teams]
            [bracketbird.pages.settings :as settings]
            [bracketbird.pages.scores :as scores]
            [bracketbird.pages.matches :as matches]
            [bracketbird.ui-selector :as sel]
            [bracketbird.ui.styles :as s]
            [bracketbird.context :as context]
            [reagent.core :as r]))

(def m-teams {:name "TEAMS" :render #(teams/render %)})
(def m-settings {:name "SETTINGS" :render #(settings/render %)})
(def m-matches {:name "MATCHES" :render #(matches/render %)})
(def m-scores {:name "SCORES" :render #(scores/render %)})

(def m-items [m-teams m-settings m-matches m-scores])

(defn- menu-item [item selected select]
  [:span {:on-click (fn [e] (when-not selected (select item)))
          :style    (merge s/menu-item-style (when selected {:opacity 1 :cursor :auto}))}
   (:name item)])

(defn menu-panel [items selector]
  (r/create-class
    {:reagent-render

     (fn [items selector]
       [:div {:style s/menu-panel-style}
        (doall (map (fn [item] ^{:key (:name item)} [menu-item item (= (:name item) (:name (sel/selected selector))) (sel/select selector)]) items))])

     :component-did-mount
     (fn [_]
       (sel/initial-select selector items))}))

(defn render [ctx]
  (let [selector (sel/subscribe-single-selection ctx)]
    (fn [ctx]
      [:div {:style {:height "100%"}}

       [menu-panel m-items selector]
       ;content
       (when-let [{:keys [render name]} (sel/selected selector)]
         (render (context/sub-ui ctx [(keyword (clojure.string/lower-case name))])))])))
