(ns bracketbird.history
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import [goog Uri]
           [goog.history Html5History]))



(defn- get-history []
  (Html5History. js/window))

(defn enable [f]
  (doto (get-history)
    (events/listen EventType/NAVIGATE f)
    (.setEnabled true)))

(defn set-token [token]
  (-> (get-history)
      (.setToken token))
  token)

(defn get-token []
  (-> (get-history)
      (.getToken)))

