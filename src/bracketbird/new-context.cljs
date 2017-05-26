(ns bracketbird.new-context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.application-state :as app-state]
            [bracketbird.context-util :as context-util]
            [bracketbird.model.tournament :as tournament]
            [bracketbird.util :as ut]))



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
                           :team            {:parent :teams
                                             :id     :team-id}
                           :enter-team      {:parent :teams-tab}})



(defn subscribe [ctx k]
  (println "ctx" ctx)
  (let [{:keys [path used-ids] :as path-m} (context-util/build-ctx-path context-levels ctx k)]

    (prn "subscribe" path-m)



    ;validate relevant context values are present

    ;build path


    ;reaction
    (reaction (get-in app-state/state path))))


(defn subscriber-ui [ctx ui-ctx k])

(defn update-ui [ctx ui-ctx k fn])

(defn dom-id [ctx ui-ctx k])

; ABOVE IS NEW

(defn add-context [ctx k v]
  ;validate new context is legal
  (assoc ctx k v))

