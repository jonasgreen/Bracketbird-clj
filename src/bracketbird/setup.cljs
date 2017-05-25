(ns bracketbird.setup
  (:require-macros [reagent.ratom :refer [reaction]]))



(def data-structure2 {:tournaments [{:id           :tournament-id
                                     :stages       [{:id :stage-id}]
                                     :stages-order nil

                                     :teams        [{:id :team-id}]
                                     :teams-order  nil
                                     }]})


(def data-structure {:tournaments {:ctx         :tournament-id
                                   :stages      {:ctx     :stage-id
                                                 :matches {:ctx    :match-id
                                                           :result {}}
                                                 :teams   nil}
                                   :stage-order nil         ;shadow of teams in a vector

                                   :teams       {:ctx :team-id}
                                   :teams-order nil         ;shadow of teams in a vector
                                   }})












(def pages {:front-page  {:tabbed-panel {:teams    {:team {}}
                                         :settings {}}}

            :tournaments {:ctx             :tournament-id
                          :tournament-page {:tabbed-panel {:teams-tab    {:team {:ctx :team-id}}
                                                           :settings-tab {}}}}

            })

(def ui-structure {:front-page      {:type :page}

                   :tournament-page {:type     :page
                                     :children {:tabs {:type :tabbed-panel

                                                       }}

                                     }
                   })


(def data-context-paths {:team  {:params [:tournament-id :team-id]
                                 :fn     (fn [{:keys [data-ctx]}] [:tournaments (:tournament-id data-ctx) :teams (:team-id data-ctx)])}

                         :teams (fn [ids] [:tournaments (:tournament-id ids) :teams])})





(defn subscribe [ctx k]

  ;validate relevant context values are present

  ;build path


  ;reaction
  (reaction (get-in [:tournaments 1 :teams 23] {})))


(defn dispatch [ctx k v])


(defn add-ctx [ctx k v])


(defn render-team [{:keys [tournament-id team-id] :as ctx}]
  (let [team (subscribe ctx :team)]
    (fn [ctx]
      [:div {:on-click #(dispatch ctx :delete-team team)}])
    )

  )

(defn render-teams [{:keys [tournament-id] :as ctx}]
  (let [teams (subscribe ctx :teams-order)]
    (fn [ctx]
      (map (fn [t] (-> ctx (add-ctx :team t) render-team) teams)))))

(defn paths [] {:team "[*team-id*]"})

(defn- doku-ctx-paths [k m]
  (loop [value (seq [])
         kkey k]

    (let [retval (conj value kkey)
          {:keys [parent id]} (get m k)]
      (if-not parent
        (str retval)
        (recur (conj retval (if id (str "*" id "*") kkey)) parent)

        )

      )
    )

  )


(defn build []
  (fn [ops]
    [:id '(:id ops)]))

(def ui-skeleton {:tournaments
                  [{:teams-page    {:teams      [{:team-name    {}
                                                  :team-seeding {}}]
                                    :enter-team {}}
                    :settings-page {}
                    :matches-page  {}
                    :scores-page   {}}
                   ]})

(def ui-display {:tournaments  {:render nil}
                 :tournament   nil
                 :teams-page   {:inherits :page}
                 :teams        {:subscriptions [:teams]}

                 :team         {:parameters [:team]
                                :render     []}

                 :team-name    {}
                 :team-seeding {}

                 }

  )
