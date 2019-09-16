(ns bracketbird.util
  (:require [clojure.string :as string]))


(defn ui-hook? [h]
  (and (keyword? h) (string/starts-with? (name h) "ui")))

(defn scroll-data [element]
  {:scroll-top    (.-scrollTop element)
   :scroll-height (.-scrollHeight element)
   :client-height (.-clientHeight element)})

(defn put-scroll-data [f]
  (fn [scroll-event]
    (->> scroll-event .-target scroll-data (f :put! merge))))

(defn scroll-to-end [scroll-data]
  (let [{:keys [scroll-height client-height]} scroll-data]
    (assoc scroll-data :scroll-top (- scroll-height client-height))))

(defn update-scroll! [element scroll-data]
  (set! (.-scrollTop element) (:scroll-top scroll-data)))

(defn scroll-elm-to-end [elm]
  (->> elm
       scroll-data
       scroll-to-end
       (update-scroll! elm)))

