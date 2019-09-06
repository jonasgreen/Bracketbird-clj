(ns bracketbird.ui-services
  (:require [bracketbird.tournament-api :as tournament-api]
            [bracketbird.event-dispatcher :as event-dispatcher]
            [bracketbird.ui :as ui]))


(defn dispatch-event
  ([event-type ctx m] (dispatch-event event-type ctx m {}))
  ([event-type ctx m {:keys [state-coeffect post-render]}]

   ;validate input - rough validation (spec)
   ;check if to warn about tournament state
   ;if warn - warn and proceed or stop

   ;else create event and execute it
   ;return handle to ui to see state of event - or let state be reflected in data model entity

   (println "Ui-services ctx : " ctx " m: " m)

   (let [{:keys [validate-input validate-state mk-event]} (get tournament-api/events-spec event-type)

         ;(validate-input ctx m)
         ;(validate-state ctx m)

         event (-> (mk-event ctx m)
                   (assoc :event-type event-type))

         events-path (-> (ui/hook-path :hooks/application ctx)
                         (conj :tournament-events))


         aggregate-path (ui/hook-path :hooks/tournament ctx)
         execute-event (-> tournament-api/events-spec
                           (get event-type)
                           :execute-event)]

     (event-dispatcher/dispatch-client-event {:event              event
                                              :execute-event      execute-event
                                              :events-path        events-path
                                              :aggregate-path     aggregate-path

                                              :aggregate-coeffect tournament-api/update-state
                                              :state-coeffect     (if state-coeffect state-coeffect identity)
                                              :post-render        (if post-render post-render identity)}))))






(defn create-tournament [])

;----------------------

(defn change-page-event [{:keys [ctx ui-hook] :as values} target page]
  {:ctx    ctx
   :src    ui-hook
   :target target
   :page   page
   :fn     (fn [state _]
             (let [app-path (ui/hook-path :hooks/application ctx)]
               (fn [state] (assoc-in state (conj app-path :active-page) page))))})

(defn change-application-page [values to-page]
  (change-page-event values :hooks/application to-page))

