(ns restyle.core)

(defonce ^:private style-definitions (atom {}))

(defn setup [style-map]
  (reset! style-definitions style-map))

(defn- resolve-value [value]
  (let [r (-> value meta :rs-resolve)]
    (if r
      ;first resolve children
      (->> value
           (reduce-kv (fn [m k v] (assoc m k (resolve-value v))) {})
           (r))

      value)))

(defn- color-to-str [{:keys [red green blue opacity]}]
  (str "rgba(" red ", " green ", " blue ", " opacity ")"))

(defn- border-to-str [{:keys [border-width border-style border-color]}]
  (str (str border-width "px " border-style " " border-color)))

(defn rgba [r g b opacity]
  ^{:rs-resolve color-to-str}
  {:red r :green g :blue b :opacity opacity})

(defn border [border-width border-style border-color]
  ^{:rs-resolve border-to-str}
  {:border-width border-width :border-style border-style :border-color border-color})


(defn resolve-style [styles value params]


  (cond (keyword? value)
        (do
          (when-let [fv (get styles value)]
            (resolve-style styles fv params)))

        (vector? value)
        (do
          (if (fn? (first value))
            ;function
            (let [children (vec (rest value))
                  ps (->> children (mapv (fn [c] (resolve-style styles c nil))))]
              (apply (first value) ps))
            ;keyword
            (resolve-style styles (first value) (rest value))))


        ;anonymous functions in styles must return maps
        (fn? value)
        (do
          (let [[p & kvs :as opts] params
                ;; TODO validate p and kvs
                additional-styling (apply hash-map (if (map? p) kvs opts))
                temp-result (if (map? p) (value p) (value))]

            (-> (resolve-style styles temp-result nil)
                (merge additional-styling))))

        ;maps with meta should not be solved until the end
        (and (map? value) (not (meta value)))
        (do
          (let [inherit (:inherit value)
                ;support multiple inheritance (vectors in vector)
                inherit-xs (when inherit (if (vector? (first inherit)) inherit (sequence [inherit])))
                resolved-inheritance (->> inherit-xs (map #(resolve-style styles % nil)) (apply merge))

                ;resolve values of map
                resolved-map (->> (dissoc value :inherit)
                                  (reduce-kv
                                    (fn [m k v] (assoc m k (if (vector? v)
                                                             (resolve-style styles v nil) v)))
                                    {}))]

            (merge resolved-inheritance resolved-map)))

        :else value))

(defn resolve-final-values [final-style-map]
  (reduce-kv (fn [m k v] (assoc m k (resolve-value v))) {} final-style-map))

(defn style [k & params]
  (-> @style-definitions
      (resolve-style k params)
      ;colors and the like are resolved into css strings
      resolve-final-values))
