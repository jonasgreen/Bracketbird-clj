(ns bracketbird.model-util)


(defn get-entity [entities e-id]
  (some (fn [c] (when (= e-id (:entity-id c)) c)) entities))