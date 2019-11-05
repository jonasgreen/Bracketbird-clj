(ns bracketbird.util
  (:require [bracketbird.dom :as d]))


(defn scroll-data [element]
  {:scroll-top    (.-scrollTop element)
   :scroll-height (.-scrollHeight element)
   :client-height (.-clientHeight element)})


(defn scroll-to-bottom [scroll-data]
  (let [{:keys [scroll-height client-height]} scroll-data
        scroll-top (- scroll-height client-height)]
    (assoc scroll-data :scroll-top scroll-top)))

(defn update-scroll-top! [element scroll-data]
  (set! (.-scrollTop element) (:scroll-top scroll-data)))

(defn scroll-elm-to-bottom! [elm]
  (->> elm
       scroll-data
       scroll-to-bottom
       (update-scroll-top! elm)))

(defn value [e] (.. e -target -value))

(defn icon
  ([icon-name] [icon {} icon-name])
  ([opts icon-name] [:i (merge-with merge {:style {:font-family             "Material Icons"
                                                   :font-weight             "normal"
                                                   :font-style              "normal"
                                                   :font-size               "14px"
                                                   :display                 "inline-block"
                                                   :width                   "1em"
                                                   :height                  "1em"
                                                   :line-height             "1"
                                                   :text-transform          "none"
                                                   :letter-spacing          "normal"
                                                   :word-wrap               "normal"
                                                   ;Support for all WebKit browsers.
                                                   :-webkit-font-smoothing  "antialiased"
                                                   ;Support for Safari and Chrome.
                                                   :text-rendering          "optimizeLegibility"
                                                   ;Support for Firefox.
                                                   :-moz-osx-font-smoothing "grayscale"
                                                   ;Support for IE.
                                                   :font-feature-settings   "liga"}} opts) icon-name]))