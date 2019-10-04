(ns tools.util)


(defn ->print [value description]
  (println description value)
  value)

(defn ->>print [description value]
  (println description value)
  value)