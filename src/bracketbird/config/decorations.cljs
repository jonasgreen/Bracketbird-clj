(ns bracketbird.config.decorations
  (:require [recontain.core :as rc]))


(def decorations {:change {:on-change (fn [data] (rc/put! (rc/sub-name data "value") (.. (:rc-event data) -target -value)))
                           :value     (fn [data] (rc/ls (rc/sub-name data "value")))}

                  :hover  {:on-mouse-enter (fn [data] (rc/put! (rc/sub-name data "hover?") true))
                           :on-mouse-leave (fn [data] (rc/put! (rc/sub-name data "hover?") false))}

                  :focus  {:on-focus (fn [data] (rc/put! (rc/sub-name data "focus?") true))
                           :on-blur  (fn [data] (rc/put! (rc/sub-name data "focus?") false))}


                  :active {:on-mouse-down (fn [data] (rc/put! (rc/sub-name data "active?") true))
                           :on-mouse-up   (fn [data] (rc/put! (rc/sub-name data "active?") false))}

                  :scroll {:on-scroll (fn [data] (let [t (.-target (:rc-event data))
                                                       scroll-top (.-scrollTop t)
                                                       scroll-height (.-scrollHeight t)
                                                       client-height (.-clientHeight t)]

                                                   (rc/put!
                                                     (rc/sub-name data "scroll-top") scroll-top
                                                     (rc/sub-name data "scroll-height") scroll-height
                                                     (rc/sub-name data "client-height") client-height
                                                     (rc/sub-name data "scroll-bottom") (- scroll-height scroll-top client-height))))}})