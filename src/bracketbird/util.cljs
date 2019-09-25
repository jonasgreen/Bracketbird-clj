(ns bracketbird.util
  (:require [clojure.string :as string]
            [bracketbird.dom :as d]))


(defn ui-hook? [h]
  (and (keyword? h) (string/starts-with? (name h) "ui")))

(defn scroll-data [element]
  {:scroll-top    (.-scrollTop element)
   :scroll-height (.-scrollHeight element)
   :client-height (.-clientHeight element)})


(defn scroll-to-bottom [scroll-data]
  (let [{:keys [scroll-height client-height]} scroll-data]
    (assoc scroll-data :scroll-top (- scroll-height client-height))))

(defn update-scroll-top! [element scroll-data]
  (set! (.-scrollTop element) (:scroll-top scroll-data)))

(defn scroll-elm-to-bottom! [elm]
  (->> elm
       scroll-data
       scroll-to-bottom
       (update-scroll-top! elm)))

(defn value [e] (.. e -target -value))

(defn index-of
  ([item xs] (index-of 0 item xs =))

  ([item xs condition-fn] (index-of 0 item xs condition-fn))

  ([index item xs condition-fn]
   (cond
     (empty? xs) -1
     (condition-fn item (first xs)) index
     :else (recur (inc index) item (rest xs) condition-fn))))

(defn previous [item xs]
  (let [index (index-of item xs)]
    (when (> index 0) (nth xs (dec index)))))

(defn after [item xs]
  (let [index (index-of item xs)]
    (when (and
            (not= -1 index)
            (< index (- (count xs) 1)))
      (nth xs (inc index)))))

(defn cyclic-previous [item xs]
  (cond
    (nil? item) (last xs)
    (= item (first xs)) (last xs)
    :else (nth xs (dec (index-of item xs)))))

(defn cyclic-next [item xs]
  (cond
    (nil? item) (first xs)
    (= item (last xs)) (first xs)
    :else (nth xs (inc (index-of item xs)))))

(defn insert [item index xs]
  (let [[before after] (split-at index xs)]
    (vec (concat before [item] after))))

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