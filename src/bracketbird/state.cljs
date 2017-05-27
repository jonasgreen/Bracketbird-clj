(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [bracketbird.context-util :as context-util]))



(defonce state (r/atom {:tournaments {}
                        :pages       {:values {:active-page :front-page}}}))


(def context-levels {:tournaments      {:parent   nil
                                        :ctx-type :model}
                     :tournament       {:parent :tournaments
                                        :id     :tournament-id}
                     :stages           {:parent :tournament}
                     :stage-ids        {:parent :tournament}
                     :stage            {:parent :stages
                                        :id     :stage-id}
                     :matches          {:parent :stage}
                     :match-ids        {:parent :stage}
                     :match            {:parent :matches
                                        :id     :match-id}
                     :result           {:parent :match
                                        :id     :result-id}
                     :teams            {:parent :tournament}
                     :team-ids         {:parent :tournament}
                     :team             {:parent :teams
                                        :id     :team-id}
                     ; UI
                     :pages            {:parent   nil
                                        :ctx-type :ui}
                     :front-page       {:parent :pages}
                     :tournament-page  {:parent :pages}
                     ;tabs
                     :teams-tab        {:parent :tournament-page}
                     :teams-table      {:parent :teams-tab}
                     :team-row         {:parent :teams
                                        :id     :team-id}
                     :enter-team-input {:parent :teams-tab}})

(def context-levels-local-state)


(def ui-data {:pages2 (r/atom {:tournament-page (r/atom {:scroll    23
                                                         :teams-tab (r/atom {:teams-table (r/atom {})
                                                                             :enter-team  (r/atom {})})})})})

(defn subscribe [ctx k]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    ;validate relevant context values are present

    ;build path

    ;reaction
    (reaction (get-in @state path))))


(defn query [ctx k]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    ;validate relevant context values are present

    ;build path

    (get-in @state path)))

(defn update! [ctx k fn]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    (println "update" path)
    (swap! state update-in path fn)))

(defn dom-id [ctx k])

; ABOVE IS NEW

(defn add-ctx [ctx k v]
  ;validate new context is legal
  (assoc ctx k v))

