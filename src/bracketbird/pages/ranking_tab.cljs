(ns bracketbird.pages.ranking-tab
  (:require [bracketbird.ui.styles :as s]
            [bracketbird.pages.tournament-tab-content :as tab-content]))








(defn- content [ctx]
  [:div "scores-tab"]
  )


(defn render [ctx]
  [tab-content/render ctx [content ctx]])