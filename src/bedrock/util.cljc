(ns bedrock.util
  (:require [clojure.string]
            [clojure.walk :as walk]
            [cljs.pprint]
            [clojure.string :as string]))

(defn soft-join
  "Join a list of elements into a presentation-friendly string"
  [delim xs]
  (->> xs (map str) (filter seq) (clojure.string/join delim)))

(defn guard
  "Guards a value by predicate on said value"
  [pred x]
  (when (pred x) x))

(defn partial>>
  "As partial, but slices the bound args in AFTER the unbound ones"
  [f & args]
  (fn [& xs] (apply f `(~@xs ~@args))))

(defn partial-1
  "As partial, but slices the bound args in after the first unbound one"
  [f & args]
  (fn [x & xs] (apply f `(~x ~@args ~@xs))))

(defn partial-2
  "As partial, but slices the bound args in after the second unbound one"
  [f & args]
  (fn [x y & xs] (apply f `(~x ~y ~@args ~@xs))))

(defn flatten-1-clean
  "Flattens a seq by 1 level and removes nils"
  [xs]
  (guard seq (apply concat (for [x xs] (when x (if (sequential? x) (remove nil? x) [x]))))))

(defn deep-merge
  "Deep-merges zero or more maps. (Subject to change)"
  [& args]
  (when args (let [[m & rest] args]
               (cond (map? m) (apply merge-with deep-merge args)
                     rest (apply merge-with deep-merge rest)
                     :else m))))

(defn merge-proper
  "As merge, but does not overwrite with values that are nil"
  [m & maps]
  (apply merge m (map (partial filter second) maps)))

(defn assoc-where
  "As assoc, but overwrites only values where predicate holds"
  ([_] nil)
  ([_ m] m)
  ([pred m & next] (loop [m m, [k v & next] next]
                     (let [m (if (pred (get m k)) (assoc m k v) m)]
                       (if next (recur m next) m))
                     )))

(defn get-time
  "Get current time in milliseconds"
  []
  #?(:clj (System/currentTimeMillis) :cljs (.getTime (js/Date.))))

(defn stupid-sleep
  "Stupid sleep routine that consumes CPU-cycles"
  [ms]
  (let [t (+ ms (get-time))]
    (while (< (get-time) t))))

(defn pp-str
  "pprint to a string"
  [x]
  (with-out-str (#?(:cljs cljs.pprint/pprint :clj clojure.pprint/pprint) x)))

(defn pp-str'
  "pprint to a string, shortening functions. nb! for human consumption only.
   The optional ns-or-kw argument determines a namespace that is printed as ::.
   If ns is a string, it's treated as a namespace-name, if it's a keyword, the
   namespace of the keyword is understood. If it's :short namespaces are shortened"
  [x & [ns]]
  (let [ns (cond (string? ns) ns
                 (= :short ns) ns
                 (keyword? ns) (namespace ns)
                 :else :full)]
    (->> x (walk/prewalk #(cond (fn? %) (symbol "#(...)")
                                (keyword? %) (cond (nil? (namespace %)) %
                                                   (= ns :short) (symbol (str "::/" (name %)))
                                                   (= ns (namespace %)) (symbol (str "::" (name %)))
                                                   :else %)
                                :else %)) pp-str)))

(defn prn-str'
  "identity with (println pp-str' % %2) as side effect"
  [x & [opt]]
  (println (pp-str' x opt)) x)

(defn kw-lowercase
  "Turn a keyword into lower-case"
  [kw]
  (->> kw str rest (apply str) string/lower-case keyword))

(defn comp>
  "Composes a list functions for application from left to right, treating nil as identity"
  [& fns]
  (->> fns (map #(or % identity)) reverse (apply comp)))

(defn comp-or [pred1 pred2]
  "Composes two predicates to be the inclusive or'ing predicate of the two"
  (fn [pred1 pred2] (cond (not pred1) pred2
                          (not pred2) pred1
                          :else (fn [v] (or (pred1 v) (pred2 v))))))
(defn comp-and [pred1 pred2]
  "Composes two predicates to be the and'ing predicate of the two"
  (fn [pred1 pred2] (cond (not pred1) pred2
                          (not pred2) pred1
                          :else (fn [v] (and (pred1 v) (pred2 v))))))

(declare map-keys)
(defn replace-template [text seq]
  (let [m (->> (map vector (range) seq)
               (into {})
               (map-keys (comp keyword str)))]
    (clojure.string/replace text #"\{\w+}"
                            (comp m keyword clojure.string/join butlast rest))))

(defn assoc-some [m & kvs]
  (->> (partition-all 2 kvs)
       (filter (fn [[_ v]] (some? v)))
       (apply concat)
       (apply assoc m)))

(defn- graph-collect-tree
  "Collects the keys of a tree of nodes starting from start-key and enumerated by child-lister.
  Optional flag :pre-order / :post-order
  :pre-order is default"
  [map-graph child-lister start-key & flags]
  (let [f (fn f [m k]
            (let [xs (map (partial f (dissoc m k))
                          (child-lister (get m k)))]
              (if (:post-order (set flags))
                (concat xs [k])
                (concat [k] xs))
              ))]
    (->> start-key (f map-graph) flatten distinct)))

(defn keyaware-merge-with
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping(s)
  from the latter (left-to-right) will be combined with the mapping in
  the result by calling (f key val-in-result val-in-latter)."
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (first e) v (second e)]
                          (if (contains? m k)
                            (assoc m k (f k (get m k) v))
                            (assoc m k v))))
          merge2 (fn [m1 m2]
                   (reduce merge-entry (or m1 {}) (seq m2)))]
      (reduce merge2 maps))))

(defn outer-join-with
  "As outer join, but takes a merging function that is applied to combine
   all 'rows' that result in the same key-value, (from left to right).

   Example:

  (outer-join-with vector
                   (comp even? :a)
                   [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
                   [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ([{:a 1, :b 10, :c 100} {:a 3, :b 30, :d 300}]
       [{:a 2, :b 20, :c 200} {:a 2, :b 5, :c 500}
        {:a 2, :b 20, :d 200} {:a 2, :b 25, :d 200}])

  (outer-join-with (comp count list)
                   :a
                   [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}]
                   [{:a 2 :b 20 :d 200}{:a 3 :b 30 :d 300}])
  ==> (1 2 1)

  (outer-join-with (comp + :c)
                   :a
                   [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}]
                   [{:a 2 :b 20 :d 200}{:a 3 :b 30 :d 300}])
   ==> (100 200 nil)"

  [merge-fn key-fn & colls]
  (->> colls
       (map (partial group-by key-fn))
       (apply (partial merge-with concat))
       vals
       (map (partial apply merge-fn))))

(defn outer-join
  "Outer join of zero or more collections of maps ('rows' or rather: merge'able values),
  by a key-function.
  When two or more maps with the same key value (ie. value of applying the
  key-function to the map), they are merged in order from left to right.

  Be aware that maps where the key-function returns nil are included.
  Duplicates are not removed.

  Examples:

  (outer-join :a [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}]
                 [{:a 2 :b 20 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 1, :b 10, :c 100} {:a 2, :b 20, :c 200, :d 200} {:a 3, :b 30, :d 300})

  (outer-join :a [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}]
                 [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 1, :b 10, :c 100} {:a 2, :b 25, :c 200, :d 200} {:a 3, :b 30, :d 300})

  (outer-join :x [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}]
                 [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 3, :b 30, :c 200, :d 300})

  (outer-join :a [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
                 [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 1, :b 10, :c 100} {:a 2, :b 25, :c 500, :d 200} {:a 3, :b 30, :d 300})

  (outer-join :b [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
                 [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 1, :b 10, :c 100} {:a 2, :b 20, :c 200, :d 200}
       {:a 2, :b 5, :c 500} {:a 2, :b 25, :d 200} {:a 3, :b 30, :d 300})

  (outer-join (juxt :a :b)
              [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
              [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 1, :b 10, :c 100} {:a 2, :b 20, :c 200, :d 200} {:a 2, :b 5, :c 500}
       {:a 2, :b 25, :d 200} {:a 3, :b 30, :d 300})

  (outer-join (constantly nil)
            [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
            [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}])
  ==> ({:a 3, :b 30, :c 500, :d 300})

  (outer-join (comp even? :a)
              [{:a 1 :b 10 :c 100}{:a 2 :b 20 :c 200}{:a 2 :b 5 :c 500}]
              [{:a 2 :b 20 :d 200}{:a 2 :b 25 :d 200}{:a 3 :b 30 :d 300}]
  ==> ({:a 3, :b 30, :c 100, :d 300} {:a 2, :b 25, :c 500, :d 200})"
  [key-fn & colls]
  (apply outer-join-with merge key-fn colls))

(defn cross-join
  "Cross join of over two collections, with optional
  predicate (row1,row2 -> bool) and merge-function.
  Default merge-function is the standard merge, making the
  function suitable for non-relation operations too.
  Duplicates are not removed.

  Example:

  (cross-join [{:a 1} {:a 2}] [{:b 1} {:b 2} {:b 3}])
  ==> ({:a 1, :b 1} {:a 1, :b 2} {:a 1, :b 3}
       {:a 2, :b 1} {:a 2, :b 2} {:a 2, :b 3})

  (cross-join [] [{:b 1} {:b 2} {:b 3}])
  ==> nil

  (cross-join [{:a 1 :x 'one} {:a 2 :x 'two}]
              [{:b 1} {:b 2 :x 'this :y 'is-it} {:b 3 :x 'that}])
  ==> ({:a 1, :x one, :b 1} {:a 1, :x this, :b 2, :y is-it} {:a 1, :x that, :b 3}
       {:a 2, :x two, :b 1} {:a 2, :x this, :b 2, :y is-it} {:a 2, :x that, :b 3}

  (cross-join [{:a 1 :x 'one} {:a 2 :x 'two}]
              [{:b 1} {:b 2 :x 'this :y 'is-it} {:b 3 :x 'that}]
              #(= (:a %) (:b %2)))
  ==> ({:a 1, :x one, :b 1} {:a 2, :x this, :b 2, :y is-it})

  (cross-join [{:a 1 :x 'one} {:a 2 :x 'two}]
              [{:b 1} {:b 2 :x 'this :y 'is-it} {:b 3 :x 'that}]
              #(=(:a %) (:b %2))
              #(assoc % :z %2))
  ==> ({:a 1, :x one, :z {:b 1}} {:a 2, :x two, :z {:b 2, :x this, :y is-it}})

  (cross-join [3 5 7 8] [100 200 300 401] #(even? %2) +)
  ==> (103 203 303 105 205 305 107 207 307 108 208 308)"
  [rel1 rel2 & [where merge-fn]]
  (seq (for [r1 rel1 r2 rel2
             :when (if where (where r1 r2) true)]
         ((or merge-fn merge) r1 r2))))

(defn ?call
  "Applies f to args if f is a function, otherwise returns f"
  [f & args]
  (if (fn? f) (apply f args) f))

(defn ?transform
  "Applies f to args if f is a function, otherwise returns first arg"
  [f & [arg0 :as args]]
  (if (fn? f) (apply f args) arg0))

(defn ?deref "Derefs argument if possible, otherwise just returns it unchanged"
  [x]
  (if #?(:cljs (satisfies? IDeref x)
         :clj  (instance? clojure.lang.IDeref x))
    @x x))

(defn call-when
  "If condition holds, transform args by applying f to args, otherwise return first arg"
  [condition f arg0 & rest]
  (if condition (apply f arg0 rest) arg0))

(defn listify
  "Promotes a proper non-sequential to a list of one element, and turns non-list sequentials into seq's"
  [x]
  (cond (list? x) x
        (sequential? x) (seq x)
        (nil? x) nil
        :else (list x)))

(defn cartesian-product
  "Cartesian product of ore or more sequences. Threats scalars as lists of one element"
  ([] nil)
  ([x] (map list (listify x)))
  ([x & xs] (for [a (listify x) b (apply cartesian-product xs)] (conj b a))))

(defn ?set
  "Turn argument into a set, if it isn't one already.
  nil is turned into an empty set."
  [x]
  (cond (set? x) x
        (nil? x) #{}
        :else #{x}))

(defn plurality?
  "Is argument a collection with a plurality of elements?"
  [x]
  (try (> (count x) 1)))

(defn ?side-effector "Makes an identity function with optional side-effects and optional extra parameters"
  [& side-effects]
  (fn [a & args] (doseq [f side-effects :when (fn? f)]
                   (apply f a args)) a))

(defn side-effect "Performs side-effect f on argument-list, then returns first arg"
  [f a & rest]
  (do (apply f a rest) a))

(defn do-map
  "As map, but for side-effects instead of result. Returns the last of the collections to map over"
  [f & args]
  (doall (apply map f args))
  (last args))

(defn add-reagent-keys
  "Adds reagent keys to a sequence. Keys will be added where none exist (as meta-data or [:div {:key xxx}]) , and existing keys will not be reused"
  [xs]
  (let [x (->> xs (map #(or (:key (meta %)) (when (vector? %) (:key (second %))) 1)) (apply max 1))]
    (map-indexed (fn [i h] (if (:key (meta h))
                             h
                             (with-meta h {:key (+ i x)})))
                 xs)))

(defn hiccup-table "Creates a hiccup table"
  [{:keys [rows f-cell table-attrib tr-attrib td-attrib]}]
  [:table (?call table-attrib rows)
   [:tbody (map-indexed
             (fn [r row]
               [:tr (merge {:key r} (?call tr-attrib r row))
                (map-indexed
                  (fn [c cell]
                    [:td (merge {:key c} (?call td-attrib r c cell))
                     ((or f-cell identity) cell r c)])
                  row)])
             rows)]])

(defn wrapper "Makes a function wrapper with optional pre and post processing"
  [& [f-pre f-post]]
  (fn [& [f]] (comp (or f-post identity) (or f identity) (or f-pre identity))))

(defn walk-collect
  "Walks a form, collecting all elements where predicate holds"
  [pred form] (let [a (atom nil)]
                (clojure.walk/postwalk
                  (fn [e] (when (pred e) (swap! a conj e)) e)
                  form)
                @a))

(defn walker
  "Makes a walker-function."
  [& {:keys [get-children update-children pre-children post-children pre-node post-node pre-root post-root root-wrapper]}]
  (letfn [(walker [node] (when node (->> node
                                         ((or pre-node identity))
                                         walk-children
                                         ((or post-node identity)))))
          (walk-children [node]
            (let [children (cond get-children (get-children node)
                                 (sequential? node) node
                                 (map? node) (:children node)
                                 :else nil) _ (prn node :==> children)
                  children' (into (empty children)
                                  (when (and (sequential? children) (seq children))
                                    (((or (wrapper pre-children post-children) identity)
                                       (partial map walker)) children)))
                  update-children (cond update-children update-children
                                        (sequential? node) (comp second list)
                                        (map? node) #(assoc % :children %2)
                                        :else identity)]
              (if children
                (update-children node children')
                node)))]
    (->> walker
         ((wrapper pre-root post-root))
         ((or root-wrapper identity)))))

(defn flatten-seq
  "Flatten a sequence.

  Example:

  (flatten-seq :sub
    [{:id 12
      :sub [{:id 5 :sub [{:id 67} {:id 99}]}
            {:id 50}]}])
  ==> ({:id 12, :sub [{:id 5, :sub [{:id 67} {:id 99}]}
                    {:id 50}]}
       {:id 5, :sub [{:id 67} {:id 99}]}
       {:id 67}
       {:id 99}
       {:id 50})"
  [get-children xs]
  ((fn f [xs] (mapcat (fn [x]
                        (conj (mapcat f [(get-children x)])
                              x))
                      xs)) xs))

(defn walk-seq
  "Walks a sequence of maps, updates children under key k.

  Example:

  (walk-seq
       :sub
       (fn [this parent] (assoc this :par (:id parent)))
       [{:id 12
         :sub [{:id 5 :sub [{:id 67} {:id 99}]}
        {:id 50}]}])
  ==> ({:id 12, :sub ({:id 5
                       :sub ({:id 67, :par 5}
                             {:id 99, :par 5})
                       :par 12}
      {:id 50, :par 12})
      :par nil})"
  [k transform xs]
  ((fn f [xs parent]
     (let [f' (fn [x] (let [x (transform x parent)]
                        (if (k x)
                          (update x k f x)
                          x)))]
       (map f' xs)))
    xs nil))

(defn provide! [state path id f] (let [{id' :id, v :v} (get @state path)]
                                   (if (= id id')
                                     v
                                     (let [v (f)]
                                       (swap! state update-in path assoc :id id :v v) v))))

;; ---------------------------------------------------------------
;; Semi-standard fns defined here due to src-path practicalities.
;; Hesitate before you dedupe.
;; ---------------------------------------------------------------

(defn map-keys
  "Maps a function over the keys of an associative collection."
  [f coll]
  (into {} (for [[k v] coll] [(f k) v])))
(defn map-vals
  "Maps a function over the values of an associative collection."
  [f coll]
  (into {} (for [[k v] coll] [k (f v)])))
