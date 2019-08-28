(ns bracketbird.ui)


(defn select [m tab]
  (-> m
      (assoc :previous-selected (:selected m))
      (assoc :selected tab)))
