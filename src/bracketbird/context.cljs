(ns bracketbird.context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.application-state :as app-state]))

(defn mk [scope id]
  {:scope scope :id id})

(defn ctx-path [{:keys [scope id]}]
  (if scope [scope id] [id]))

(defn ui-path [ctx]
  (into (ctx-path ctx) [:ui-state]))

(defn data-path [ctx]
  (into (ctx-path ctx) [:data]))

;----- get -------

(defn data [ctx]
  (get-in @app-state/state (data-path ctx)))

(defn ui [ctx]
  (get-in @app-state/state (ui-path ctx)))

;------ swap data ----

(defn swap-data! [ctx data]
  (swap! app-state/state assoc-in (data-path ctx) data))

;---- subscribe -----

(defn subscribe-data [ctx path]
  (reaction (get-in @app-state/state (into (data-path ctx) path))))
