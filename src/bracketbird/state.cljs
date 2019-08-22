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

(def ui-context-paths {:front-page      [:ui :pages :front-page]
                       :tournament-page [:ui :pages :tournament-page :tournament-id]

                       :teams-tab       [:ui :pages :tournament-page :tournament-id :tabs :teams-tab]
                       :stages-tab      [:ui :pages :tournament-page :tournament-id :tabs :stages-tab]
                       :matches-tab     [:ui :pages :tournament-page :tournament-id :tabs :matches-tab]
                       :ranking-tab     [:ui :pages :tournament-page :tournament-id :tabs :ranking-tab]

                       :stage-component [:ui :tournaments :tournament-id :pages :stages-page :stage-id :stage-component]

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

(def context-levels-local-state)


(def ui-data {:pages2 (r/atom {:tournament-page (r/atom {:scroll    23
                                                         :teams-tab (r/atom {:teams-table (r/atom {})
                                                                             :enter-team  (r/atom {})})})})})


(defn mk-path [k ctx]
  (let [unresolved-path (get context-paths k)]
    (if (nil? unresolved-path)
      (throw (js/Error. (str "Unable to find mapping for " k " in unresolved context paths: " context-paths)))
      (->> unresolved-path
           (map (fn [k] (get ctx k k)))
           (vec)))))

(defn subscribe
  ([path]
   (reaction (get-in @state path)))
  ([k ctx] (-> (mk-path k ctx)
               (subscribe))))

(defn query
  ([path]
   (get-in @state path))
  ([k ctx] (-> (mk-path k ctx)
               (query))))



(defn old-subscribe [k ctx]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    (println "subscribe" path)

    ;validate relevant context values are present

    ;build path

    ;reaction
    (reaction (get-in @state path))))




(defn update! [k ctx fn]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)
        start-time (.getTime (js/Date.))]

    (println "start time" start-time)
    (r/next-tick #(println "next-tick" (.getTime (js/Date.))))

    (r/after-render #(println "******* RENDER TIME *****" (- (.getTime (js/Date.)) start-time)))
    (swap! state update-in path fn)))

(defn dom-id [ctx k])

; ABOVE IS NEW

