(ns bracketbird.config.ranking-page)


(def ranking-page {:hook   :ranking-page
                   :ctx    [:application-id :tournament-id]
                   :render (fn [_] [:div "scores-tab"])})
