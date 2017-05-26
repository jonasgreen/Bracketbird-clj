(ns bracketbird.context-util)


(defn- build-ctx-path [context-structure ctx c-key]
  (loop [k c-key
         path []
         used-ids #{}]

    (let [{:keys [parent id]} (get context-structure k)]
      (if-not parent
        {:path (vec (cons k path)) :used-ids used-ids}
        (recur parent
               (cons (if id (get ctx id) k) path)
               (if id (conj used-ids id) used-ids))))))




