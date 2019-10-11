(ns bracketbird.config.matches-page)


(def matches-page {:container-name :matches-page
                   :ctx            [:application-id :tournament-id]
                   :render         (fn [_] [:div "matches-tab"])})
