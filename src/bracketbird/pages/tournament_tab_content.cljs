(ns bracketbird.pages.tournament-tab-content
  (:require [bracketbird.ui.ui-scroller :as scroll]
            [bracketbird.ui.panels :as p]
            [bracketbird.ui.styles :as s]))

(defn render [ctx content]
  (let [scroller (scroll/subscribe ctx)]
    (fn [ctx]
      [p/scroll-panel {:scroller scroller
                       :style    (merge s/tournamet-tab-content-style
                                        (when (scroll/has-scroll scroller)
                                          {:border-top "1px solid rgba(241,241,241,1)"}))}
       content ctx])))