(ns bracketbird.util)


(defn entity [entities e-id]
  (some (fn [e] (when (= e-id (:entity-id e)) e)) entities))

(defn index-and-entity
  "Returns [index-of-entity entity]"
  [entities e-id]
  (first (keep-indexed (fn [i e] (when (= e-id (:entity-id e)) [i e])) entities)))