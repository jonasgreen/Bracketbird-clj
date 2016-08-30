(ns bracketbird.application-ui
  (:require [bracketbird.ui.frontpage :as front-page]
            [bracketbird.contexts.application-context :as app-context]
            [bracketbird.ui.frontpage :as front-page]))







(defn render [state]
   (println "render application" @state)
  (let [navigation (app-context/subscribe-navigation)]
    [front-page/render @navigation]))