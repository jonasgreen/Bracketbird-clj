(ns bracketbird.util)



(defn from-to [m1 m2 k & ks]
  (merge m2 (select-keys m1 (conj ks k))))