(ns bracketbird.application
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]))


(defonce test-ids {})


(defn test? [] (get-in @state/state [:application :test]))

(defn unique-id [k]
  (if (test?)
    (-> test-ids
        (update k inc)
        (get k))
    (ut/squuid)))