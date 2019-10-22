(ns bracketbird.config.matches-page)


(def matches-page {:config-name :matches-page
                   :ctx            [:application-id :tournament-id]
                   :render         (fn [_ _] [:div "matches-tab"])})
