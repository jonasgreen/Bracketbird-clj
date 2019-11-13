(ns recontain.impl.config-stack
  (:require [recontain.impl.state :as rc-state]))

(defn- replace-one-size-vector-key [k]
  (if (and (vector? k) (= 1 (count k)))
    (first k)
    k))

(defn- replace-one-size-vector-keys [config]
  (->> config (reduce-kv (fn [m k v] (assoc m (replace-one-size-vector-key k) v)) {})))

(defn size [config-stack]
  (count (:configs config-stack)))

(defn bind-config-value
  "Config values are (if they are functions) bound at a certain stack-index (used when calling super) and a certain handle (used to get local-state).

   Later at execution time, when the stack has grown. Config values are bound to the whole stack and the input data (element-data).
   Additional parameters (besides config-stack and element-data) can be given. This is what happens when calling rc/call..)

   Execution of the config function can happen in two ways:
   If the the key of the config value is a symbol (function at component level) then only parameters are passed in.
   Else only element data is passed in.
  "
  [handle stack-atom k v]
  (if (fn? v)
    ;;binding for later execution
    (fn [element-data & params]
      (binding [rc-state/*current-handle* (assoc handle :config-stack stack-atom
                                                        :rc-data element-data)]

        (if (symbol? k)
          (apply v params)
          (v element-data))))
    v))


(defn bind-config-values [stack-atom handle config]
  (->> config
       replace-one-size-vector-keys
       (reduce-kv (fn [m k v] (assoc m k (bind-config-value handle stack-atom k v))) {})))



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


(defn- explode-config [config]
  (let [render-fn (get config [:render])
        exploding-keys (-> config (dissoc [:render]) keys (filter vector?))
        exploded-config (->> exploding-keys
                             (reduce (fn [m k] (doseq [[k1 v1] (get k config)]
                                                 (assoc m (into k (vec k1)) v1))) {}))]

    (assoc exploded-config [:render] render-fn)))

(defn add-config [config-stack handle config-name config]
  (let [stack-atom (atom (-> config-stack
                             (update :config-names conj config-name)
                             (update :shaved conj [])))]

    (->> (explode-config config)
         (bind-config-values stack-atom handle)
         (swap! stack-atom update :configs conj))))

(defn mk [handle config-name config]
  (-> {:shaved       []
       :config-names []
       :configs      []}
      (add-config handle config-name config)))


(defn- shave-config [config element-ref preserve-symbols?]
  (let [shaved-config (reduce-kv (fn [m k v] (cond
                                               (and (vector? k) (= (first k) element-ref) (< 1 (count k)))
                                               (assoc m (subvec k 1) v)

                                               (and (symbol? k) preserve-symbols?)
                                               (assoc m k v)

                                               :else
                                               m))
                                 {}
                                 config)]
    shaved-config))

(defn- shave [config-stack element-ref preserve-symbols?]
  (-> config-stack
      (update :configs (fn [xs] (->> xs
                                     (map #(-> %
                                               (shave-config element-ref preserve-symbols?)
                                               replace-one-size-vector-keys))
                                     vec)))
      (update :shaved (fn [v] (->> v (map #(conj % element-ref)) vec)))))

(defn shave-by-component [config-stack component-ref]
  (shave config-stack component-ref false))

(defn shave-by-element [config-stack component-ref]
  (shave config-stack component-ref true))

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