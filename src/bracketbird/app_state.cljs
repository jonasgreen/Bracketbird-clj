(ns bracketbird.app-state
  (:require [reagent.core :as r]))


(defonce state (r/atom {:figwheel-reloads 0}))



(def not-ready :state/not-ready)
(def ready :state/ready)
(def active :state/active)

;Only Tournament, stages, groups can be in this one
(def done :state/done)
(def finish :state/finish)
