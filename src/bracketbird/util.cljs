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
  ([item coll] (index-of 0 item coll =))

  ([item coll condition-fn] (index-of 0 item coll condition-fn))

  ([index item coll condition-fn]
   (cond
     (empty? coll) -1
     (condition-fn item (first coll)) index
     :else (recur (inc index) item (rest coll) condition-fn))))


(defn cyclic-previous [item coll]
  (cond
    (nil? item) (last coll)
    (= item (first coll)) (last coll)
    :else (nth coll (dec (index-of item coll)))))

(defn cyclic-next [item coll]
  (cond
    (nil? item) (first coll)
    (= item (last coll)) (first coll)
    :else (nth coll (inc (index-of item coll)))))
