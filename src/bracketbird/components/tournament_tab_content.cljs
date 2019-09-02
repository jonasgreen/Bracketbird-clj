(ns bracketbird.components.tournament-tab-content
  (:require [bracketbird.styles :as s]
            [bracketbird.state :as state]
            [reagent.core :as r]
            [goog.events :as events]))









(defn scroll-fn [ctx]
    (fn [position]
      #_(context/update-ui! ctx position)))

(defn subscribe [ctx] {}
    #_(let [s-ctx nil #_(context/sub-ui-ctx ctx [:scroll])]
      {:position (context/subscribe-ui s-ctx)
       :scroll   (scroll-fn s-ctx)}))

(defn position [s]
    (let [p (:position s)]
      (if p @p 0)))

(defn scroll [s p]
    ((:scroll s) p))

(defn has-scroll [s]
    (< 0 (position s)))



(defn scroll-panel [ui-path]
  (let [values (state/hook ui-path {:scroll-position 0})
        listener (fn [e] (-> @values
                             (assoc :scroll-position (.-scrollTop (.-target e)))
                             (state/update-ui!)))]
    (r/create-class
      {:reagent-render
       (fn [opts content]
         [:div opts content])

       :component-did-mount
       (fn [this]
         (events/listen (r/dom-node this) "scroll" listener)
         (set! (.-scrollTop (r/dom-node this)) (:scroll-position @values)))

       :component-will-unmount
       (fn [this]
         (events/unlisten (r/dom-node this) "scroll" listener))})))




(defn render [ctx content]
  (let [scroller (subscribe ctx)]
    (fn [ctx]
      [scroll-panel {:scroller scroller
                       :style    (merge s/tournamet-tab-content-style
                                        (when (has-scroll scroller)
                                          {:border-top "1px solid rgba(241,241,241,1)"}))}
       content ctx])))