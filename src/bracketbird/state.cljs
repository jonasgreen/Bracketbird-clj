(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [bracketbird.context-util :as context-util]))


(defonce state (r/atom {:tournaments {}
                        :pages {:page :front-page}}))


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
    (reaction (get-in @state path))))

(defn query [ctx k]
  (let [{:keys [path used-ids] :as path-m} (context-util/build-ctx-path context-levels ctx k)]

    ;validate relevant context values are present

    ;build path

    ;reaction
    (get-in @state path)))

(defn update! [ctx k fn])

(defn dom-id [ctx k])

; ABOVE IS NEW

(defn add-ctx [ctx k v]
  ;validate new context is legal
  (assoc ctx k v))

