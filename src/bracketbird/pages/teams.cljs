(ns bracketbird.pages.teams
  (:require [bracketbird.ui.styles :as s]
            [reagent.core :as r]
            [goog.events :as events]
            [bracketbird.ui-scroller :as scroll]))





(defn scroll-panel [{:keys [scroller] :as opts} _]
  (let [state-atom (atom nil)
        listener (fn [e] (println "scrolling"))]
    (r/create-class
      {:reagent-render
       (fn [opts content]
         [:div opts content])

       :component-did-mount
       (fn [this]
         ;(events/listen (r/dom-node this) "scroll" listener)
         (scroll/initial-scroll scroller))

       :component-will-unmount
       (fn[_]
         ;(event)
         (scroll/scroll @state-atom))})))



(defn render-content [ctx]
  (println "render conent2")
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
  (println "render temas33")
  (let [scroller (scroll/subscribe ctx)]
    (fn [ctx]
      [scroll-panel {:scroller scroller
                     :style    (merge s/menu-panel-style


                                      (when (scroll/has-scroll scroller)
                                        {:border-top "1px solid rgba(241,241,241,1)"}))}
       (render-content ctx)])))