(ns bracketbird.contexts.tournament-context
  (:require [bracketbird.app-state :as state]
            [bracketbird.contexts.context :as ctx]))

;------ service-events -------

(defrecord Load-tournament [t-id]
  ctx/IService-event
  (-execute-service! [this data]
    {:teams []
     :events []})

  (-handle-service-result [this data result]
    result))

;----- business events ----------

(defrecord create-team-event [event-id team-id team-name]
  ctx/IBusiness-event
  (-execute-business [this data]))


(defn next-event-id [tournament-ctx]
  (count (ctx/get-in-ctx tournament-ctx [:events])))

;-------------------------
; event-wrapper-functions
;-------------------------
(defn create-team [t-ctx team-name]
  (ctx/dispatch t-ctx (->create-team-event :create-team (next-event-id t-ctx) team-name)))


;-------- ctx --------------

(defrecord Tournament-context [tournament-id]
  ctx/IContext
  (-state [this] state/state)
  (-root-path [this] [:ctx-kontrol])
  (-id [this] (keyword (str tournament-id)))
  (-load [this]
    (ctx/dispatch this (->Load-tournament tournament-id)))

  (-unload [this]
    (ctx/remove-context! this)))

;----- subscriptions ----

(defn subscribe-teams [ctx]
  (ctx/subscribe ctx [:teams]))


