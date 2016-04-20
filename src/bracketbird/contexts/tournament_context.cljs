(ns bracketbird.contexts.tournament-context
  (:require [bracketbird.app-state :as app-state]
            [bracketbird.model.tournament :as t-m]
            [bracketbird.model.uuid :as uuid]
            [bracketbird.contexts.context :as ctx]))

;------ service-events -------

(defrecord Load-tournament [t-id]
  ctx/IService-event
  (-execute-service! [this data]
    {:teams  []
     :events []})

  (-handle-service-result [this data result]
    result))

;----- business events ----------

(defrecord create-tournament-event [event-key tournament-id]
  ctx/IBusiness-event
  (-execute-business [this data]))


(defrecord create-team-event [event team-name]
  ctx/IBusiness-event
  (-execute-business [this tournament]))




;-------- ctx functions -----------

(defn next-event-id [t-ctx]
  (count (ctx/get-in-ctx t-ctx [:events])))

(defn tournament-id [t-ctx]
  (ctx/-id t-ctx))

(defn client-event [t-ctx event-type model-id]
  {:tournament-id (ctx/-id t-ctx)
   :model-id      model-id
   :type          event-type
   ;set by server
   :event-id      nil})

;-------------------------
; event-wrapper-functions
;-------------------------
(declare ->Tournament-context)

(defn create-tournament []
  (let [t-ctx (->Tournament-context uuid/squuid)]
        (ctx/dispatch t-ctx (map->create-tournament-event (client-event t-ctx :create-tournament (uuid/squuid))))))

(defn create-team [t-ctx team-name]
  (ctx/dispatch t-ctx (map->create-team-event (merge {:team-name team-name} (client-event t-ctx :create-team (uuid/squuid))))))


;-------- ctx --------------

(defrecord Tournament-context [tournament-id]
  ctx/IContext
  (-state [this] app-state/state)
  (-root-path [this] [:ctx-tournament])
  (-id [this] (keyword (str tournament-id)))
  (-load [this]
    (ctx/dispatch this (->Load-tournament tournament-id)))

  (-unload [this]
    (ctx/remove-context! this)))

;----- subscriptions ----

(defn subscribe-teams [ctx]
  (ctx/subscribe ctx [:teams]))


