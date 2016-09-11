(ns bracketbird.pages.teams
  (:require [bracketbird.ui.styles :as s]
            [reagent.core :as r]
            [goog.events :as events]
            [bracketbird.ui-scroller :as scroll]))





(defn scroll-panel [{:keys [scroller] :as opts} _]
  (let [state-atom (atom nil)
        listener (fn [e] (println (.-scrollTop (.-target e))))]
    (r/create-class
      {:reagent-render
       (fn [opts content]
         [:div opts content])

       :component-did-mount
       (fn [this]
         (events/listen (r/dom-node this) "scroll" listener)
         (scroll/initial-scroll scroller))

       :component-will-unmount
       (fn[this]
         (events/unlisten (r/dom-node this) "scroll" listener)
         (scroll/scroll scroller @state-atom))})))



(defn render-content [ctx]
  [:div
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   [:div "teams"]
   ]

  )

(defn render [ctx]
  (let [scroller (scroll/subscribe ctx)]
    (fn [ctx]
      [scroll-panel {:scroller scroller
                     :style    (merge s/page-style


                                      (when (scroll/has-scroll scroller)
                                        {:border-top "1px solid rgba(241,241,241,1)"}))}
       (render-content ctx)])))