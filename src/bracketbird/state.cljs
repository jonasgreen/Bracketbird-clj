(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [bracketbird.context_util :as context-util]))


(defonce state (r/atom {:system       {}
                        :applications {}
                        :ui           {}}))




(def context-paths {:application         [:applications :application-id]
                    :tournament          [:applications :application-id :tournament]

                    :teams               [:applications :application-id :tournament :teams]
                    :team                [:applications :application-id :tournament :teams :teams-id]

                    :stages              [:applications :application-id :tournament :stages]
                    :stages-order        [:applications :application-id :tournament :stages-order]
                    :stage               [:applications :application-id :tournament :stages :stage-id]

                    ;notice these are matches in a given stage
                    :stage-matches       [:applications :application-id :tournament :stages :stage-id :matches]
                    :stage-matches-order [:applications :application-id :tournament :stages :stage-id :matches :matches-order]
                    :stage-match         [:applications :application-id :tournament :stages :stage-id :matches :match-id]
                    })


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


(defn mk-path [k ctx]
  (let [unresolved-path (get context-paths k)]
    (if (nil? unresolved-path)
      (throw (js/Error. (str "Unable to find mapping for " k " in unresolved context paths: " context-paths)))
      (->> unresolved-path
           (map (fn [p] (get ctx p p)))
           (vec)))))

(defn subscribe
  ([path]
   (reaction (get-in @state path)))
  ([k ctx] (-> (mk-path k ctx)
               (subscribe))))

(defn subscribe-ui-values
  ([path] (subscribe-ui-values path {}) )
  ([path not-found] (reaction (get-in @state (conj path :values) not-found))))

(defn update-ui-values! [path values]
  (swap! state assoc-in (conj path :values) values))


(defn query
  ([path]
   (get-in @state path))
  ([k ctx] (-> (mk-path k ctx)
               (query))))

(defn dom-id [ctx k])

; ABOVE IS NEW

