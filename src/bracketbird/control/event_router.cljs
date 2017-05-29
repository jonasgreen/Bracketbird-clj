(ns bracketbird.event-router
  (:require [reagent.ratom :as ratom]
            [bracketbird.tournament-api_old :as t-api]
            [bracketbird.state :as state]
            [bracketbird.util :as ut]

            [bracketbird.model.tournament :as tournament]
            [bracketbird.context-util :as context-util]))


(defn- tournament-path [ctx]
  (context-util/path state/context-levels ctx :tournament))


(defn build-teams []
  (mapv (fn[i] {:team-id (str "team" i)}) (range 1000)))


(def api {:create-tournament {:ctx-level :tournaments
                              :params    [:tournament-id]
                              :validate  (fn [state ctx values] true) ;todo
                              :execute   (fn [state ctx {:keys [tournament-id]}]
                                           (let [path (context-util/path state/context-levels {} :tournaments)]
                                             (state/update! :tournaments ctx (fn [m]
                                                                               (println "type t-id" tournament-id)
                                                                               (assoc (or m {}) (str tournament-id) {:teams (build-teams)})))
                                             ))}


          :create-team       {:ctx-level :tournament
                              :params    [:team-name :team-id]
                              :validate  (fn [state ctx values] true) ;todo
                              :execute   (fn [state ctx values]
                                           (update-in state (tournament-path ctx) #(tournament/add-team % values)))}

          :update-team-name  {:ctx-level :team
                              :type      :update
                              :params    [:team-name]
                              :fn        (fn [ctx-root-model])
                              }
          })



(defn pending-packet [router-id event]
  {:id          (ut/squuid)
   :router-id   router-id
   :count       nil                                         ;(+ (count (:packets @state-atom)) (count (:out @state-atom)))
   :level-count nil                                         ;1
   :event       event})

(defn- send [state event bumb-level?])



(defn dispatch
  "Dispatches a client event"
  [ctx api-key values]
  ;validations

  (let [event {:id     (ut/squuid)
               :type   api-key
               :ctx    ctx
               :values values}])

  ;short-cutted directly to api, when there is no server when there is no server
  (let [{:keys [execute] :as api} (get api api-key)
        new-state (execute @state/state ctx values)]
    (reset! state/state new-state)))




(defn dispatch-old [ctx api-event]
  (t-api/execute-api-event ctx api-event))

(defn create-router [id subscriber]
  (let [state (ratom/atom {:router-id       id
                           :subscriber      subscriber
                           :history-packets []
                           :pending-packets []})]
    (fn [event bump-level?]
      (if (empty? (:pending-events @state))
        (send state event bump-level?)
        (swap! state update-in [:pending-packets] conj (pending-packet id event))))))

