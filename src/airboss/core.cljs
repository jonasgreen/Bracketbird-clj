(ns airboss.core
  (:require [airboss.state-view :as state-view]
            [airboss.design-view :as design-view]))

(defn load-state-viewer [m]
  (state-view/load m {}))


(defn load-design-viewer []
  (design-view/load))