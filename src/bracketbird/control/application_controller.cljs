(ns bracketbird.application-controller
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.application-state :as app-state]
            [bracketbird.api.application-api_old :as app-api]
            [bracketbird.tournament-controller :as t-ctrl]
            [bracketbird.history :as history]))

(defn- front-page-ctx []
  {:page :front-page
   :ctx  nil})

(defn- tournament-page-ctx [t-ctx]
  {:page :tournament-page
   :ctx  t-ctx})

(defn- error-page-ctx []
  {:page :error-page
   :ctx  nil})

(defn- mk-page-context [token]
  (cond
    (clojure.string/blank? token) (front-page-ctx)
    (= (str (int token)) token) (tournament-page-ctx (t-ctrl/mk-ctx token))
    :else (error-page-ctx)))


(defn on-navigation-changed []
  (app-api/update-page-context (-> (history/get-token)
                                   (mk-page-context))))

(defn enable-history []
  (history/enable on-navigation-changed))

(defn create-tournament []
  (let [t-id (-> (t-ctrl/count-tournaments) inc str)
        t-ctx (t-ctrl/mk-ctx t-id)
        t-page-ctx (tournament-page-ctx t-ctx)]

    (history/set-token t-id)
    (t-ctrl/create-tournament t-ctx)
    (app-api/update-page-context t-page-ctx)))

(defn trigger-ui-reload []
  (app-api/reload-ui))

(defn get-state-atom []
  app-state/state)

;---------------
; subscriptions
;---------------

(defn subscribe-page-context []
  (reaction (get @app-state/state :page-context)))

