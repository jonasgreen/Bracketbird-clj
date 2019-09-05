(ns bracketbird.util
  (:require [clojure.string :as string]))



(defn ui-hook? [h]
  (and (keyword? h) (string/starts-with? (name h) "ui")))