(ns recontain.impl.config-stack)

(defn print-config-stack [config-stack]
  (println "---- CONFIG STACK ----")
  (doseq [index (range (count (:config-names config-stack)))]
    (println index (get-in config-stack [:config-names index]) (get-in config-stack [:configs index]))))

(defn mk [config-name config]
  {:config-names [config-name]
   :configs      [config]})


(defn add [config-stack config-name config]
  (-> config-stack
      (update :config-names conj config-name)
      (update :configs conj config)))


(defn- shave-config [config element-ref]
  (let [shaved-config (reduce-kv (fn [m k v] (if (and (vector? k) (= (first k) element-ref))
                                               (assoc m (subvec k 1) v)
                                               m))
                                 {}
                                 config)]
    shaved-config))

(defn shave [config-stack element-ref]
  (update config-stack :configs (fn [xs] (->> xs
                                              (map #(shave-config % element-ref))
                                              vec))))


(defn prepare-config-for-element [config element-ref]
  (let [prepared (reduce-kv (fn [m k v] (if (vector? k)
                                          (if (= (first k) element-ref)
                                            (assoc m (last k) v)
                                            m)
                                          (assoc m k v)))
                            {}
                            config)]
    prepared))

(defn prepare-for-element [config-stack element-ref]
  (let [after (update config-stack :configs (fn [xs] (->> xs
                                              (map #(prepare-config-for-element % element-ref))
                                              vec)))]
    after))



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
     (when (= (count (:configs config-stack)) index)
       (throw (js/Error. (str "Index out of bounds - unable to find config-key: "option-key))))
     (let [value (get-in config-stack [:configs index option-key])]
       (if value
         {:value value :index index}
         (recur (inc index)))))))

(defn super[config-stack index config-key]


  )
