(ns bracketbird.old-context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.state :as app-state]
            [bracketbird.context-util :as context-util]))



(def context-levels {:tournaments   {:parent nil}
                     :tournament    {:parent :tournaments
                                     :id     :tournament-id}

                     :stages        {:parent :tournament}
                     :stage-order   {:parent :tournament}
                     :stage         {:parent :stages
                                     :id     :stage-id}

                     :matches       {:parent :stage}
                     :matches-order {:parent :stage}
                     :match         {:parent :matches
                                     :id     :match-id}

                     :result        {:parent :match
                                     :id     :result-id}

                     :teams         {:parent :tournament}
                     :teams-order   {:parent :tournament}
                     :team          {:parent :teams
                                     :id     :team-id}})

(def ui-context-structure {:pages           {:parent nil}
                           :tournament-page {:parent :pages}
                           :teams-tab       {:parent :tournament-page}
                           :teams           {:parent :teams-tab}
                           :team            {:parent :teams}
                           :enter-team      {:parent :teams-tab}})



(defn subscribe [ctx k]
  (println "ctx" ctx)
  (let [{:keys [path used-ids] :as path-m} (context-util/build-ctx-path context-levels ctx k)]

    (prn "subscribe" path-m)



    ;validate relevant context values are present

    ;build path


    ;reaction
    (reaction (get-in app-state/state path))))

; ABOVE IS NEW

(defn add-context [ctx k v]
  ;validate new context is legal
  (assoc ctx k v))



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


;------ util ---------

(defn update-ui-on-input-change! [ctx]
  (fn [e]
    (update-ui! ctx (.. e -target -value))))