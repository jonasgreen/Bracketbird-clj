(ns bracketbird.config.ranking-page)


(def ranking-page {:config-name :ranking-page
                   :ctx            [:application-id :tournament-id]
                   :render         (fn [_ _] [:div "scores-tab"])})
