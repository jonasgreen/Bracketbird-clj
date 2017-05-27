(ns bracketbird.application-controller
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.state :as state]
            [bracketbird.api.application-api_old :as app-api]
            [bracketbird.control.tournament-api :as tournament-api]
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
  (let [t-id (str (tournament-api/create-tournament))
        ctx (state/add-ctx {} :tournament-id t-id)]

    (history/set-token t-id)
    (state/update! {} :pages (fn [m] (assoc m :active-page :tournament-page
                                              :ctx ctx)))))

(defn trigger-ui-reload []
  (app-api/reload-ui))

(defn get-state-atom []
  state/state)

;---------------
; subscriptions
;---------------

(defn subscribe-page-context []
  (reaction (get @state/state :page-context)))

