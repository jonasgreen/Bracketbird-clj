(ns bracketbird.ui-services
  (:require [bracketbird.control.tournament-api :as tournament-api]
            [bracketbird.util :as ut]
            [bracketbird.event-dispatcher :as event-dispatcher]
            [clojure.string :as string]
            [bracketbird.state :as state]))


(defn- execute
  ([event-type ctx m] (execute event-type ctx m identity))
  ([event-type ctx m update-fn]

   ;validate input - rough validation (spec)
   ;check if to warn about tournament state
   ;if warn - warn and proceed or stop

   ;else create event and execute it
   ;return handle to ui to see state of event - or let state be reflected in data model entity

   (let [{:keys [validate-input validate-state mk-event]} (get tournament-api/events-spec event-type)
         event (-> (mk-event ctx m)
                   (assoc :event-type event-type)
                   (assoc :event-name (->> event-type
                                           (map name)
                                           reverse
                                           (string/join " "))))

         events-path (-> (state/mk-path :tournament ctx)
                         (conj :tournament-events))

         aggregate-path (state/mk-path :tournament ctx)
         execute-event (get tournament-api/events-spec event-type)]

     ;(validate-input ctx m)
     ;(validate-state ctx m)

     (event-dispatcher/dispatch-client-event {:event              event
                                              :execute-event      execute-event
                                              :events-path        events-path
                                              :aggregate-path     aggregate-path

                                              :aggregate-coeffect tournament-api/update-state
                                              :state-coeffect     update-fn}))))



(defn create-tournament [ctx]
  (let [app-path (state/mk-path :application ctx)]
    (execute [:tournament :create]
             ctx
             {}
             (fn [state] (assoc-in state (conj app-path :active-page) :tournament-page)))))

(defn create-team [ctx team-name]
  (execute [:team :create]
           ctx
           {:team-name team-name}))








