(ns bracketbird.setup)



(def data-structure {:tournaments nil
                     :tournament  {:parent :tournaments
                                   :id     :tournament-id}

                     :stages      {:parent :tournament}
                     :stage-order {:parent :tournament}     ;shadow of teams in a vector
                     :stage       {:parent :stages
                                   :id     :stage-id}

                     :teams       {:parent :tournament}
                     :teams-order {:parent :tournament}     ;shadow of teams in a vector
                     :team        {:parent :teams
                                   :id     :team-id}})

(def data-context-paths {:team  {:params [:tournament-id :team-id]}
                         :fn    (fn [{:keys [data-ctx]}] [:tournaments (:tournament-id data-ctx) :teams (:team-id data-ctx)])

                         :teams (fn [ids] [:tournaments (:tournament-id ids) :teams])})


(defn paths {:team "[*team-id*]"})

(defn- doku-ctx-paths [k m]
  (loop [value (seq [])
         kkey k]

    (let [retval (conj value kkey)
          {:keys [parent id]} (get m k)]
      (if-not parent
        (str retval)
        (recur (conj retval (if id (str "*"id"*") kkey)) parent)

        )



      )
    )



  )

(def data-context-paths




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
