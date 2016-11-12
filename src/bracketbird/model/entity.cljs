(ns bracketbird.model.entity)


(defprotocol IEntity
  (-id [this]))

(defn- ensure-id [e]
  (if (satisfies? IEntity e) (-id e) e))

(defn same?
  "When e1 and e2 is the same, e2 is returned as logical true"
  [e1 e2]
  (when (= (ensure-id e1) (ensure-id e2)) e2))

(defn- same?-keep-index
  "When e1 and e2 is the same, i is returned as logical true"
  [e1 i e2]
  (when (same? e1 e2) i))

(defn entity [entities e]
  (some (partial same? e) entities))

(defn index-of-entity
  [entities e]
  (->> entities
       (keep-indexed (partial same?-keep-index e))
       first))

(defn split-at-entity [entities e]
  (when-let [index (index-of-entity entities e)]
    (->> entities
         (split-at index))))

(defn previous-entity [entities e]
  (-> (split-at-entity entities e)
      first
      last))

(defn next-entity [entities e]
  (-> (split-at-entity entities e)
      last
      rest
      first))

(defn remove-entity [entities e]
  (remove (partial same? e) entities))

(defn insert [entities index e]
  (let [[before after] (split-at index entities)]
    (vec (concat before [e] after))))
