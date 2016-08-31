(ns bracketbird.pages.tournament-page
  (:require [bracketbird.tournament-controller :as t-ctrl]))



(defn render [ctx]
  (let [teams @(t-ctrl/subscribe-teams ctx)]
    [:div (str ctx)
     [:button {:on-click #(t-ctrl/add-team ctx)} "add team"]
     (map-indexed (fn [i t] ^{:key i}[:pre (str t)]) teams)]))
