(ns bracketbird.context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.application-state :as app-state]))

(def ctx-id :ctx-id)
(def ctx-scope :ctx-scope)
(def ctx-path :ctx-path)
(def ui-path :ui-path)
(def data-path :data-path)

(defn mk [scope id]
  (let [c-path (if scope [scope id] [id])]
    {ctx-id    id
     ctx-scope scope
     ctx-path  c-path
     data-path (into c-path [:data])
     ui-path   (into c-path [:ui])}))

(defn data [ctx]
  (get-in @app-state/state (data-path ctx)))

(defn ui [ctx]
  (get-in @app-state/state (ui-path ctx)))

(defn sub-ui-ctx [ctx path]
  (->> (into (ui-path ctx) path)
       (assoc ctx ui-path)))

;------ swap data ----

(defn swap-data! [ctx data]
  (swap! app-state/state assoc-in (data-path ctx) data))

(defn update-ui! [ctx data]
  (swap! app-state/state assoc-in (ui-path ctx) data))

;---- subscribe -----

(defn subscribe-data [ctx path]
  (reaction (get-in @app-state/state (into (data-path ctx) path))))

(defn subscribe-ui [ctx]
  (reaction (get-in @app-state/state (ui-path ctx))))
