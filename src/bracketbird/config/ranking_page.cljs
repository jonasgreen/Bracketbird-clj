(ns bracketbird.config.ranking-page)


(def ranking-page {:config-name :ranking-page
                   :ctx            [:application-id :tournament-id]
                   :render         (fn [_] [:div "scores-tab"])})
