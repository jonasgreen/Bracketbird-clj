(ns bracketbird.context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [bracketbird.application-state :as app-state]
            [bracketbird.context-util :as context-util]
            [bracketbird.util :as ut]))



(def context-levels {:tournaments      {:parent nil}
                     :tournament       {:parent :tournaments
                                        :id     :tournament-id}
                     :stages           {:parent :tournament}
                     :stage-ids        {:parent :tournament}
                     :stage            {:parent :stages
                                        :id     :stage-id}
                     :matches          {:parent :stage}
                     :matches-ids      {:parent :stage}
                     :match            {:parent :matches
                                        :id     :match-id}
                     :result           {:parent :match
                                        :id     :result-id}
                     :teams            {:parent :tournament}
                     :team-ids         {:parent :tournament}
                     :team             {:parent :teams
                                        :id     :team-id}
                     ; UI
                     :pages            {:parent nil}
                     :front-page       {:parent :pages}
                     :tournament-page  {:parent :pages}
                     ;tabs
                     :teams-tab        {:parent :tournament-page}
                     :teams-table      {:parent :teams-tab}
                     :team-row         {:parent :teams
                                        :id     :team-id}
                     :enter-team-input {:parent :teams-tab}})



(defn subscribe [ctx k]
  (println "ctx" ctx)
  (let [{:keys [path used-ids] :as path-m} (context-util/build-ctx-path context-levels ctx k)]

    (prn "subscribe" path-m)

    ;validate relevant context values are present

    ;build path

    ;reaction
    (reaction (get-in app-state/state path))))


(defn update! [ctx k fn])

(defn dom-id [ctx k])

; ABOVE IS NEW

(defn add [ctx k v]
  ;validate new context is legal
  (assoc ctx k v))

