(ns airboss.core
  (:require [airboss.state-view :as state-view]
            [airboss.design-view :as design-view]))


(def state (atom {}))

(defn load-state-viewer [m]
  (if (:state-viewer-is-loaded @state)
    (.warn js/console "trying to load airboss.state-viewer multiple times")
    (do (swap! state assoc :state-viewer-is-loaded true)
        (state-view/load m {}))))


(defn load-design-viewer []
  (if (:design-viewer-is-loaded @state)
    (.warn js/console "trying to load airboss.design-viewer multiple times")
    (do (swap! state assoc :design-viewer-is-loaded true)
        (design-view/load))))