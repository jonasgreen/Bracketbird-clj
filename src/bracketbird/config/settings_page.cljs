(ns bracketbird.config.settings-page)



(def settings-page {:hook   :settings-page
                    :ctx    [:application-id :tournament-id]
                    :render (fn [_] [:div "settings tab"])})
