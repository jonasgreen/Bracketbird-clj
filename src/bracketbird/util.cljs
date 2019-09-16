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

(defn scroll-to-end [element]
  (let [{:keys [scroll-height client-height]} (scroll-data element)]
    (set! (.-scrollTop element) (- scroll-height client-height))))