(ns bracketbird.ui-services
  (:require [recontain.core :as rc]
            [bracketbird.tournament-api :as tournament-api]
            [bracketbird.event-dispatcher :as event-dispatcher]
            [bracketbird.util :as ut]))


(defn dispatch-event [{:keys [ctx event-type content state-coeffect post-render] :as m}]

  ;validate input - rough validation (spec)
  ;check if to warn about tournament state
  ;if warn - warn and proceed or stop

  ;else create event and execute it
  ;return handle to ui to see state of event - or let state be reflected in data model entity

  (println "Ui-services:" m)

  (let [{:keys [validate-input validate-state mk-event]} (get tournament-api/events-spec event-type)

        ;(validate-input ctx m)
        ;(validate-state ctx m)

        event (-> (mk-event ctx content)
                  (assoc :event-type event-type))

        events-path (-> (rc/hook-path :hook/application ctx)
                        (conj :tournament-events))

        aggregate-path (rc/hook-path :hook/tournament ctx)

        execute-event (-> tournament-api/events-spec
                          (get event-type)
                          :execute-event)]

    (event-dispatcher/dispatch-client-event {:event              event
                                             :execute-event      execute-event
                                             :events-path        events-path
                                             :aggregate-path     aggregate-path

                                             :aggregate-coeffect tournament-api/update-state
                                             :state-coeffect     (if state-coeffect state-coeffect identity)
                                             :post-render        (if post-render post-render identity)})))






;----------------------

(defn last-team [h]
  (-> h
      (rc/get-data :hook/tournament)
      tournament-api/last-team))

(defn previous-team [h team-id]
  (-> h
      (rc/get-data :hook/tournament)
      (tournament-api/previous-team team-id)))

(defn after-team [h team-id]
  (-> h
      (rc/get-data :hook/tournament)
      (tournament-api/after-team team-id)))

(defn index-of [h team-id]
  (->> (rc/get-data h :hook/teams-order)
       (ut/index-of team-id)))