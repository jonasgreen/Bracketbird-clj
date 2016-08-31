(ns bracketbird.application-state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))


(defonce state (r/atom {}))


(def not-ready :not-ready)
(def ready :ready)
(def in-progress :in-progress)
(def rank-equality :rank-equality)
(def finished :finished)




(defn subscribe-ctx [{:keys [path id] :as ctx}]
  (reaction (get state :system)))