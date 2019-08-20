(ns bracketbird.context_util)


(defn build-ctx-info [context-structure ctx c-key]
  (loop [k c-key
         path []
         used-ids #{}]

    (let [{:keys [parent id ctx-type]} (get context-structure k)]
      (if-not parent
        {:path (vec (cons k path)) :used-ids used-ids :ctx-type ctx-type}
        (recur parent
               (cons (if id (get ctx id) k) path)
               (if id (conj used-ids id) used-ids))))))

(defn path [context-structure ctx c-key]
  (:path (build-ctx-info context-structure ctx c-key)))




