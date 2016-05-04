(ns bracketbird.app-state
  (:require [reagent.core :as r]))


(defonce state (r/atom {:figwheel-reloads 0}))


(def not-ready :not-ready)
(def ready :ready)
(def in-progress :in-progress)
(def rank-equality :rank-equality)
(def finished :finished)
