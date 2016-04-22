(ns bracketbird.model-util)


(defn get-model [models id]
  (some (fn [c] (when (= id (:id c)) c)) models))