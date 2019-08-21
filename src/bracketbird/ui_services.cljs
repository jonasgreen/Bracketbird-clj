(ns bracketbird.ui-services
  (:require [bracketbird.control.tournament-api :as tournament-api]
            [bracketbird.util :as ut]
            [bracketbird.event-dispatcher :as event-dispatcher]
            [clojure.string :as string]))


(defn- execute
  ([event-type ctx m] (execute event-type ctx m identity))
  ([event-type ctx m update-fn]
   (println "execute" event-type)

   ;validate input - rough validation (spec)
   ;check if to warn about tournament state
   ;if warn - warn and proceed or stop

   ;else create event and execute it
   ;return handle to ui to see state of event - or let state be reflected in data model entity

   (let [{:keys [validate-input validate-state mk-event]} (get tournament-api/events-spec event-type)]
     ;(validate-input ctx m)
     ;(validate-state ctx m)

     (event-dispatcher/dispatch-client-event {:event                 (-> (mk-event ctx m)
                                                                         (assoc :event-type event-type)
                                                                         (assoc :event-name (->> event-type
                                                                                                 (map name)
                                                                                                 reverse
                                                                                                 (string/join " " ))))
                                              :events-spec           tournament-api/events-spec
                                              :events-execution-spec tournament-api/events-execution-spec
                                              :post-update           update-fn}))))



(defn create-tournament []
  (execute [:tournament :create]
           {}
           {}
           (fn [state] (assoc-in state [:application :active-page] :tournament-page))))

(defn create-team [{:keys [tournament-id]} team-name]
  (execute [:team :create]
           {:tournament-id tournament-id}
           {:team-name team-name}))








