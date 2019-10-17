(ns bracketbird.tournament-api
  (:require [bracketbird.system :as system]
            [bracketbird.util :as ut]
            [cljs.spec.alpha :as s]))


(def settings {:max-number-of-teams {:description "asdfasdfasdf"
                                     :type        :integer}
               :number-of-groups    {:description "asdfasdfasdf"
                                     :type        :integer}
               :ranking-rules       {:description "asdf"
                                     :type        :selector
                                     :values      []}
               :group-play          {:settings    [:max-number-of-teams :number-of-groups :number-of-repeats]
                                     :description "bla bla bla"

                                     }
               })






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


(defn mk-settings [setting-type]
  (if (= setting-type :group)

    {:settings-type       :group
     :max-number-of-teams nil
     :number-of-groups    1
     :ranking-rules       [:ranking/by-points :ranking/by-scored-goals :ranking/by-goal-diff]}

    {:settings-type       :knockout
     :max-number-of-teams nil}))

(defn mk-stage [id]
  {:stage-id      id
   :state         :not-ready
   :settings      nil
   :matches       nil
   ;:score-sheet   nil maybe only in view
   :teams-order   []
   :final-ranking []})

(defn mk-match [id]
  {:match-id id
   :state    :not-ready
   :round    nil
   :teams    []
   :result   []})




(defn update-state [tournament]
  tournament)

(defn last-team [tournament]
  (when-let [team-id (-> tournament :teams-order last)]
    (get-in tournament [:teams team-id])))

(defn previous-team [tournament team-id]
  (when-let [team-id (->> tournament :teams-order (ut/previous team-id))]
    (get-in tournament [:teams team-id])))

(defn after-team [tournament team-id]
  (when-let [team-id (->> tournament :teams-order (ut/after team-id))]
    (get-in tournament [:teams team-id])))


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
                                                              {:keys [team-name index] :as m}]

                                                           {:tournament-id tournament-id
                                                            :team-id       (system/unique-id :team)
                                                            :team-name     team-name
                                                            :index         index})

                                         :execute-event  (fn [t {:keys [team-id team-name index]}]
                                                           (let [team (mk-team team-id team-name)]
                                                             (-> t
                                                                 (update :teams assoc team-id team)
                                                                 (update :teams-order (fn [items]
                                                                                        (if index
                                                                                          (ut/insert team-id index items)
                                                                                          (conj items team-id))))
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
                                                               (update :teams-order #(->> % (remove (fn [v] (= team-id v))) vec))
                                                               (assoc :dirty true)))}

                  [:stage :create]      {:validate-input (fn [ctx m] ())
                                         :validate-state (fn [ctx m] ())
                                         :mk-event       (fn [{:keys [tournament-id]} {:keys [stage-type]}]
                                                           {:tournament-id tournament-id
                                                            :stage-id      (system/unique-id :stage)
                                                            :stage-type    stage-type})

                                         :execute-event  (fn [t {:keys [stage-id stage-type index]}]
                                                           (let [stage (-> stage-id mk-stage (assoc :settings (mk-settings stage-type)))]
                                                             (-> t
                                                                 (update :stages assoc stage-id stage)
                                                                 (update :stages-order (fn [items]
                                                                                         (if index
                                                                                           (ut/insert stage-id index items)
                                                                                           (conj items stage-id))))
                                                                 (assoc :dirty true)
                                                                 )))}
                  })
