(ns stateless.util)

(defn ->print [value description]
  (println description value)
  value)

(defn ->>print [description value]
  (println description value)
  value)

(defn index-of
  ([item xs] (index-of 0 item xs =))

  ([item xs condition-fn] (index-of 0 item xs condition-fn))

  ([index item xs condition-fn]
   (cond
     (empty? xs) -1
     (condition-fn item (first xs)) index
     :else (recur (inc index) item (rest xs) condition-fn))))

(defn previous [item xs]
  (let [index (index-of item xs)]
    (when (> index 0) (nth xs (dec index)))))

(defn after [item xs]
  (let [index (index-of item xs)]
    (when (and
            (not= -1 index)
            (< index (- (count xs) 1)))
      (nth xs (inc index)))))

(defn cyclic-previous [item xs]
  (cond
    (nil? item) (last xs)
    (= item (first xs)) (last xs)
    :else (nth xs (dec (index-of item xs)))))

(defn cyclic-next [item xs]
  (cond
    (nil? item) (first xs)
    (= item (last xs)) (first xs)
    :else (nth xs (inc (index-of item xs)))))

(defn insert [item index xs]
  (let [[before after] (split-at index xs)]
    (vec (concat before [item] after))))


;-----------------
; Console logging

(defn- js-apply [f target args]
  (.apply f target (to-array args)))

(def log
  (let [types {:log   (.-log js/console)
               :info  (.-info js/console)
               :warn  (.-warn js/console)
               :error (.-error js/console)}]

    (fn [type & args]
      (let [found (get types type)]
        (if found
          (js-apply found js/console args)
          (js-apply (.-warn js/console) js/console
                    (conj args ". You where trying to log:" (str "Logging error. Given type " type " not valid. Valid types are " (keys types)))))))))


(defn map-preserve-coll
  [f coll]
  (if (coll? coll)
    (into (empty coll) (map f coll))
    (map f coll)))


;;Use partition-by instead
(defn split-after
  "Takes a sequence and splits it into multiple sequences."
  [pred xs]
  (println "xs" xs)
  (loop [[k & ks] xs accumulated {:buffer [] :sequences []}]
    (if-not k
      (->> (conj (:sequences accumulated) (:buffer accumulated))
           (remove empty?))
      (recur ks
             (if (pred k)
               (-> accumulated
                   (assoc :buffer [])
                   (update :sequences conj (conj (:buffer accumulated) k)))
               (update accumulated :buffer conj k))))))


(defn update-when-exists
  "Takes a collection and a path into that collection, where the last item in the path should be an update function,
   and updates the item at the given path, only if items are present all the way down the path.
   Ex: (update-when-exists {:a {:b {:c 'c'}}} [:a :b :c keyword]) => {:a {:b {:c :c}}}.

   Express multiple updates in the same vector
   Ex: (update-when-exists {:a {:b {:c 'c' :d 'd'}}} [:a :b c: keyword :a :b :d keyword]) => {:a {:b {:c :c :d :d}}

   Multiple updates that share path:
   Ex: (update-when-exists {:a {:b {:c 'c' :d 'd'}}} [:a :b [c: keyword :d keyword]]) => {:a {:b {:c :c :d :d}}

   Handles sequences data structure by reducing over them.
   Ex: (update-when-exists {:a [{:b 'b'} {:c 'c'}]} [:a [:b keyword :c keyword]]) => {:a [{:b :b} {:c :c}]}

   Multi arity gives the option to leave out the outer parenthesises
   E: (update-when-exists {:a 'a' :b 'b'}]} :a keyword :b keyword) => {:a :a :b :b}
   "
  ([coll path]
   (let [split (fn [xs] (split-after #(or (fn? %) (vector? %)) xs))
         up (fn up [coll [k & ks]]
              (if k
                (cond
                  (fn? k) (k coll)
                  (vector? k) (->> (split k) (reduce up coll))
                  (sequential? coll) (map-preserve-coll #(up % (cons k ks)) coll)
                  :else (if-let [v (get coll k)]
                          (assoc coll k (up v ks))
                          coll))
                coll))]
     (->> (split path) (reduce up coll))))

  ([coll k & kfs]
   (update-when-exists coll (into [k] (vec kfs)))))

