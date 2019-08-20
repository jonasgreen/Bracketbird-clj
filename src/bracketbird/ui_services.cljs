(ns bracketbird.ui-services
  (:require [bracketbird.state :as state]
            [bracketbird.control.tournament-api :as tournament-api]
            [bracketbird.util :as ut]
            [bracketbird.event-dispatcher :as event-dispatcher]
            ))


(def services {[:tournament :create] {:validate (fn [ctx m] ())
                                      :mk-event (fn[ctx m]{:tournament-id (ut/squuid)})}


               [:team :create]       {:validate (fn [] ())
                                      :execute (fn [] ())
                                      :mk-event {}}})


(defn create-tournament []
  (let [t-id (str (tournament-api/create-tournament))]

    (state/update! :pages {} (fn [m] (assoc m :active-page :tournament-page))))



  (defn execute [service ctx m]
    (println "execute" service)

    (let [{:keys [validate mk-event]} (get-in services service)]
      (validate ctx m)
      (event-dispatcher/dispatch (mk-event ctx m))
      )

    ;validate input - rough validation (spec)
    ;check if to warn about tournament state
    ;if warn - warn and proceed or stop


    ;else create event and execute it
    ;return handle to ui to see state of event - or let state be reflected in data model entity

    ))