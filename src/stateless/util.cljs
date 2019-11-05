(ns stateless.util)


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
