(ns bracketbird.event-dispatcher
  (:require [bracketbird.util :as ut]
            [bracketbird.model.tournament :as tournament-api]
            ))


(def events {[:tournament :create] {:input-validation (fn [ctx m]())
                                    :state-validation (fn [ctx m] ())
                                    :mk-event (fn [ctx m] {:tournament-id (ut/squuid)})
                                    :target tournament-api/mk
                                    }


             [:team :create]       {:validate-input (fn [ctx m] ())
                                    :validate-state (fn [ctx m] ())
                                    :mk-event (fn[{:keys [tournament-id]} m] {:tournament-id })
                                    :target tournament-api/
                                    }})





(defn- dispatch-client-event [event]

  )

(defn- dispatch-server-event [event])





(defn dispatch [{:keys [seq-no] :as event}]
  (if seq-no
    (dispatch-server-event event)
    (dispatch-client-event event)))



