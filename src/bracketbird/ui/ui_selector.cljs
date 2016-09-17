(ns bracketbird.ui.ui-selector
  (:require [bracketbird.context :as context]))



(defn item-selector-fn [ctx]
  (fn [item]
    (context/update-ui! ctx item)))

(defn initial-item-selector-fn [ctx]
  (fn [items]
    (when-not (context/ui ctx)
      (context/update-ui! ctx (first items)))))

(defn subscribe-single-selection [ctx]
  (let [s-ctx (context/sub-ui-ctx ctx [:selected-item])]
    {:selected-item         (context/subscribe-ui s-ctx)
     :item-selector         (item-selector-fn s-ctx)
     :initial-item-selector (initial-item-selector-fn s-ctx)}))

(defn selected [s]
  (when-let [item (:selected-item s)]
    @item))

(defn select [s]
  (:item-selector s))

(defn initial-select [s items]
  (when-not (selected s))
  ((:initial-item-selector s) items))
