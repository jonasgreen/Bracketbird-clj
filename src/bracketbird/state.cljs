(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [bracketbird.context_util :as context-util]))


(defonce state (r/atom {:application {:active-page :front-page}
                        :ui          {}
                        :tournament  {}
                        :pages       {:values {:active-page :front-page}}
                        }))




(def context-structure {:tournaments {
                                      :tournament {
                                                   :id           :tournament-id
                                                   :stages       {:stage {:id      :stage-id
                                                                          :matches {:match {:id :match-id}}}}
                                                   :stages-order nil
                                                   :teams        {:team {:id :team-id}}
                                                   :teams-order  nil}}})


(def context-paths {:tournament    [:tournaments :tournament-id]

                    :teams         [:tournaments :tournament-id :teams]
                    :team          [:tournaments :tournament-id :teams :teams-id]

                    :stages        [:tournaments :tournament-id :stages]
                    :stages-order  [:tournaments :tournament-id :stages-order]
                    :stage         [:tournaments :tournament-id :stages :stage-id]

                    ;notice these are matches in a given stage
                    :matches       [:tournaments :tournament-id :stages :stage-id :matches]
                    :matches-order [:tournaments :tournament-id :stages :stage-id :matches :matches-order]
                    :match         [:tournaments :tournament-id :stages :stage-id :matches :match-id]

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


(defn subscribe [path] (reaction (get-in @state path)))

(defn old-subscribe [k ctx]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    (println "subscribe" path)

    ;validate relevant context values are present

    ;build path

    ;reaction
    (reaction (get-in @state path))))


(defn query [k ctx]
  (let [{:keys [path used-ids ctx-type] :as path-m} (context-util/build-ctx-info context-levels ctx k)
        path (if (= ctx-type :ui) (conj path :values) path)]

    ;validate relevant context values are present

    ;build path

    (get-in @state path)))

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

