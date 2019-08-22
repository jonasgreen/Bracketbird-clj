(ns bracketbird.control.tournament-api
  (:require [bracketbird.system :as system]))


(defn mk-tournament [id]
  {:tournament-id id
   :teams         []
   :stages        []
   :state         :not-ready
   :dirty         false
   :final-ranking []})

(defn mk-team [id team-name]
  {:team-id   id
   :team-name team-name})


(defn update-state [tournament]
  tournament)


(def events-spec {[:tournament :create] {:validate-input (fn [ctx m] ())
                                         :validate-state (fn [ctx m] ())
                                         ;:event-input    {}
                                         :mk-event       (fn [ctx m] {:tournament-id (system/unique-id :tournament)})
                                         :execute-event  (fn [t e]
                                                           (println "execute event" t e)
                                                           (mk-tournament (:tournament-id e)))}


                  [:team :create]       {:validate-input (fn [ctx m] ())
                                         :validate-state (fn [ctx m] ())
                                         ;:event-input    {:name :string}
                                         :mk-event       (fn [{:keys [tournament-id] :as ctx}
                                                              {:keys [team-name] :as m}]

                                                           {:tournament-id tournament-id
                                                            :team-id       (system/unique-id :team)
                                                            :team-name     team-name})

                                         :execute-event  (fn [t {:keys [team-id team-name]}]
                                                           (-> t
                                                               (update :teams conj (mk-team team-id team-name))
                                                               (assoc :dirty true)))}})
