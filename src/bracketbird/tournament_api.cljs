(ns bracketbird.tournament-api
  (:require [bracketbird.system :as system]
            [bracketbird.util :as ut]))


(def states {[:tournament :not-ready]    {}
             [:tournament :ready]        {}
             [:tournament :in-progress]  {}
             [:tournament :done-playing] {}
             [:tournament :finished]     {}
             ;-----
             [:stage :not-ready]         {}
             [:stage :ready]             {}
             [:stage :in-progress]       {}
             [:stage :done-playing]      {}
             [:stage :finished]          {}
             ;-----
             [:group :not-ready]         {}
             [:group :ready]             {}
             [:group :in-progress]       {}
             [:group :done-playing]      {}
             [:group :finished]          {}
             ;-----
             [:match :not-ready]         {}
             [:match :ready]             {}
             [:match :in-progress]       {}
             [:match :done-playing]      {}
             [:match :finished]          {}})

(defn mk-tournament [id]
  {:tournament-id id
   :teams         {}
   :teams-order   []

   :stages        {}
   :stages-order  []

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
                                         :mk-event       (fn [{:keys [tournament-id]} m]
                                                           {:tournament-id tournament-id})

                                         :execute-event  (fn [t e]
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
                                                           (let [team (mk-team team-id team-name)]
                                                             (-> t
                                                                 (update :teams assoc (:team-id team) team)
                                                                 (update :teams-order conj (:team-id team))
                                                                 (assoc :dirty true))))}
                  [:team :update]       {:validate-input (fn [ctx m] ())
                                         :validate-state (fn [ctx m] ())
                                         ;:event-input    {:name :string}
                                         :mk-event       (fn [{:keys [tournament-id team-id] :as ctx}
                                                              {:keys [team-name] :as m}]

                                                           {:tournament-id tournament-id
                                                            :team-id       team-id
                                                            :team-name     team-name})

                                         :execute-event  (fn [t {:keys [team-id team-name] :as e}]
                                                           (-> t
                                                               (update :teams assoc-in [team-id :team-name] team-name)
                                                               (assoc :dirty true)))}

                  [:team :delete]       {:validate-input (fn [ctx m] ())
                                         :validate-state (fn [ctx m] ())
                                         ;:event-input    {:name :string}
                                         :mk-event       (fn [{:keys [tournament-id team-id] :as ctx} m]
                                                           {:tournament-id tournament-id
                                                            :team-id       team-id})

                                         :execute-event  (fn [t {:keys [team-id]}]
                                                           (-> t
                                                               (update :teams dissoc team-id)
                                                               (update :teams-order #(->> % (remove (fn[v] (= team-id v))) vec))
                                                               (assoc :dirty true)))}
                  })
