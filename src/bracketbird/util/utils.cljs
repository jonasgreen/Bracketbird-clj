(ns bracketbird.util.utils)



(defn remove [v idx]
  (vec (concat (subvec v 0 idx) (subvec v (inc idx)))))

(defn entity [entities e-id]
  (some (fn [e] (when (= e-id (:entity-id e)) e)) entities))

(defn index-and-entity
  "Returns [index-of-entity entity]"
  [entities e-id]
  (first (keep-indexed (fn [i e] (when (= e-id (:entity-id e)) [i e])) entities)))

(defn index-of-entity [entities e-id]
  (first (index-and-entity entities e-id)))

(defn remove-entity [entities e-id]
  (remove entities (index-of-entity entities e-id)))
