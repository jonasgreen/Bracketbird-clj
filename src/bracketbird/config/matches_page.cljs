(ns bracketbird.config.matches-page)


(def matches-page {:hook   :matches-page
                   :ctx    [:application-id :tournament-id]
                   :render (fn [_] [:div "matches-tab"])})
