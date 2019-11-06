(ns recontain.impl.config-stack)

(def sorting (juxt (fn [v] (str (type v)))

                   (fn [v] (if (vector? v)
                             (str (type (first v)))
                             (name v)))
                   (fn [v] (if (vector? v)
                             (name (first v))
                             (name v)
                             ))
                   ))

(defn print-config-stack [config-stack]
  (println "- CONFIG STACK -")
  (doseq [index (range (count (:config-names config-stack)))]
    (let [name (get-in config-stack [:config-names index])
          configs (get-in config-stack [:configs index])]
      (println index name " - shaved:" (get-in config-stack [:shaved index]))
      (doseq [k (sort-by sorting (keys configs))]
        (println k (get configs k)))
      (println "---"))))


(defn- replace-one-size-vector-key [k]
  (if (and (vector? k) (= 1 (count k)))
    (first k)
    k))

(defn- replace-one-size-vector-keys [config]
  (->> config (reduce-kv (fn [m k v] (assoc m (replace-one-size-vector-key k) v)) {})))

(defn mk [config-name config]
  {:shaved       [[]]
   :config-names [config-name]
   :configs      [(replace-one-size-vector-keys config)]})


(defn add [config-stack config-name config]
  (-> config-stack
      (update :config-names conj config-name)
      (update :configs conj (replace-one-size-vector-keys config))
      (update :shaved conj [])))


(defn- shave-config [config element-ref]
  (let [shaved-config (reduce-kv (fn [m k v] (if (and (vector? k)
                                                      (= (first k) element-ref)
                                                      (< 1 (count k)))
                                               (assoc m (subvec k 1) v)
                                               m))
                                 {}
                                 config)]
    shaved-config))

(defn shave [config-stack element-ref]
  (-> config-stack
      (update :configs (fn [xs] (->> xs
                                     (map #(-> %
                                               (shave-config element-ref)
                                               replace-one-size-vector-keys))
                                     vec)))
      (update :shaved (fn [v] (->> v (map #(conj % element-ref)) vec)))))

(defn config-keys [config-stack]
  (->> config-stack
       :configs
       (map keys)
       flatten
       (remove nil?)

       (reduce (fn [s k] (if (keyword? k)
                           (conj s k)
                           s))
               #{})
       seq))

(defn config-value
  ([config-stack option-key]
   (config-value config-stack option-key 0))

  ([config-stack option-key start-index]
   (loop [index start-index]
     (when (> (count (:configs config-stack)) index)
       (let [value (get-in config-stack [:configs index option-key])]
         (if value
           {:value value :index index}
           (recur (inc index))))))))

(defn super [config-stack index config-key]

  )
