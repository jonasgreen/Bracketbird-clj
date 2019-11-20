(ns bracketbird.util
  (:require [recontain.core :as rc]))


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

(defn scroll-to-bottom! [sub-id & sub-ids]
  (let [elm (apply rc/dom-element sub-id sub-ids)]
    (->> elm
         scroll-data
         scroll-to-bottom
         (update-scroll-top! elm))))

