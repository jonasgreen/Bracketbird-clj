(ns bracketbird.ui.ui-scroller
  (:require [bracketbird.old-context :as context]))

(defn scroll-fn [ctx]
  (fn [position]
    (context/update-ui! ctx position)))

(defn subscribe [ctx]
  (let [s-ctx (context/sub-ui-ctx ctx [:scroll])]
    {:position (context/subscribe-ui s-ctx)
     :scroll   (scroll-fn s-ctx)}))

(defn position [s]
  (let [p (:position s)]
    (if p @p 0)))

(defn scroll [s p]
  ((:scroll s) p))

(defn has-scroll [s]
  (< 0 (position s)))