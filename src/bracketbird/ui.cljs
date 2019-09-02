(ns bracketbird.ui
  (:require [bracketbird.state :as state]))






(defn select [m tab]
  (-> m
      (assoc :previous-selected (:selected m)
             :selected tab)))




