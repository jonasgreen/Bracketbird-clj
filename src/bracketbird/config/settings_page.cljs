(ns bracketbird.config.settings-page)



(def settings-page {:container-name :settings-page
                    :ctx            [:application-id :tournament-id]
                    :render         (fn [_] [:div "settings tab"])})
