(ns bracketbird.control.tournament-api
  (:require [bracketbird.control.event-router :as event-router]
            [bracketbird.util :as ut]
            [bracketbird.model.tournament :as tournament-api]
            [bracketbird.model.team :as team]))


(defn mk-tournament [id]
  {:tournament-id id
   :teams         []
   :stages        []
   :state         :not-ready
   :dirty         false
   :history       []
   :final-ranking []})

(defn mk-team [id team-name]
  {:team-id id
   :team-name team-name})

(def events {[:tournament :create] {:input-validation (fn [ctx m] ())
                                    :state-validation (fn [ctx m] ())
                                    :mk-event         (fn [ctx m] {:tournament-id (ut/squuid)})
                                    :execute-event    (fn [t e] (mk-tournament (:tournament-id e)))}


             [:team :create]       {:validate-input (fn [ctx m] ())
                                    :validate-state (fn [ctx m] ())
                                    :mk-event       (fn [{:keys [tournament-id] :as ctx}
                                                         {:keys [team-name] :as m}]

                                                      {:tournament-id tournament-id
                                                       :team-id       (ut/squuid)
                                                       :team-name     team-name})

                                    :execute-event  (fn [t {:keys [team-id team-name]}]
                                                      (-> t
                                                          (update :teams conj (mk-team team-id team-name))
                                                          (assoc :dirty true)))
                                    }})





(defn create-tournament []
  (let [id (ut/squuid)]
    (event-router/dispatch {} :create-tournament {:tournament-id id})
    id))

(defn create-team [ctx team-name]
  (event-router/dispatch ctx :create-team {:team-id   (ut/squuid)
                                           :team-name team-name}))