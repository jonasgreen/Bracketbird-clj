(ns bracketbird.application-controller
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType])

  (:import [goog Uri]
           [goog.history Html5History]))


(defn- page-context [token]
  {:page-context :tournament-page
   :data-context {:path [:tournament] :id "123Asd"}})



(defn on-navigation-changed [token]
  (let [pc (page-context (.getFragment (Uri. js/location.href)))]
    (println "page-context" pc)))
