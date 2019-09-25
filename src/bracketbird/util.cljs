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

(defn cyclic-next [item cs]
  (cond
    (nil? item) (first cs)
    (= item (last cs)) (first cs)
    :else (nth cs (inc (index-of item cs)))))
