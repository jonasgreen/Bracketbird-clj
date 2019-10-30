(ns recontain.impl.config-stack)


(defn mk [config-name config]
  {:config-names [config-name]
   :configs      [config]})


(defn add [config-stack config-name config]
  (-> config-stack
      (update :config-names conj config-name)
      (update :configs conj config)))


(defn- shave-config [config element-ref]
  (reduce-kv (fn [m k v] (if (and (vector? k) (= (first k) element-ref))
                           (assoc m (subvec k 1) v)
                           m))
             {}
             config))

(defn shave [config-stack element-ref]
  (update config-stack :configs (fn [xs] (->> xs
                                              (map #(shave-config % element-ref))
                                              vec))))


(defn prepare-config-for-element [config element-ref]
  (reduce-kv (fn [m k v] (if (vector? k)
                           (when (= (first k) element-ref)
                             (assoc m (last k) v))
                           (assoc m k v)))
             {}
             config))


(defn prepare-for-element [config-stack element-ref]
  (update config-stack :configs (fn [xs] (->> xs
                                              (map #(prepare-config-for-element % element-ref))
                                              vec))))

(defn option-keys [config-stack]
  (->> config-stack
       :configs
       (map keys)
       concat
       (reduce (fn [s k] (if (keyword? k)
                           (conj s k)
                           s))
               #{})
       seq))

(defn option-value
  ([config-stack option-key]
   (option-value config-stack option-key 0))

  ([config-stack option-key start-index]
   (loop [index start-index]
     (let [value (get-in config-stack [:configs index option-key])]
       (if value
         {:value value :index index}
         (recur (inc start-index)))))))