(ns bracketbird.ui.panels
  (:require [reagent.core :as r]
            [goog.events :as events]
            [bracketbird.ui.ui-scroller :as scroll]))



(defn scroll-panel [{:keys [scroller]} _]
  (let [listener (fn [e] (scroll/scroll scroller (.-scrollTop (.-target e))))]
    (r/create-class
      {:reagent-render
       (fn [opts content]
         [:div opts content])

       :component-did-mount
       (fn [this]
         (events/listen (r/dom-node this) "scroll" listener)
         (set! (.-scrollTop (r/dom-node this)) (scroll/position scroller)))

       :component-will-unmount
       (fn [this]
         (events/unlisten (r/dom-node this) "scroll" listener))})))

