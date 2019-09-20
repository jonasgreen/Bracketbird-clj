(ns bracketbird.components.ranking-tab
  (:require [bracketbird.styles :as s]))




(defn render [handle state foreign-state]
  [:div {:style s/tournament-tab-content-style} "scores-tab"])