(ns bracketbird.model.tournament
  (:require [bracketbird.model.entity :as e]
            [bracketbird.model.team :as team]))

(defn mk [tournament-id]
  {:tournament-id tournament-id
   :teams         []
   :stages        []
   :dirty         false
   :history       []
   :final-ranking []})


(defn reset [t])

(defn layout-matches [t])

(defn dirtify [t]
  (assoc t :dirty true))

(defn rebuild [t]
  (-> (reset t)
      (layout-matches)))

;-------
; teams
;-------

(defn teams [t]
  (:teams t))

(defn- update-team [t team-id k value]
  (update-in t [:teams (e/index-of-entity (teams t) team-id)] assoc k value))


(defn team [t team-id]
  (e/entity (teams t) team-id))

(defn set-teams [t teams]
  (assoc t :teams (vec teams)))

(defn add-team [t {:keys [team-id team-name]}]
  (-> t
      (update :teams conj (team/mk-team team-id team-name))
      (dirtify)))

(defn insert-team [t t-id t-name index]
  (-> t
      (update :teams e/insert index (team/mk-team t-id t-name))
      (dirtify)))

(defn update-team-name [t team-id name]
  (update-team t team-id :name name))

(defn update-team-seeding [t team-id seeding]
  (-> (update-team t team-id :seeding seeding)
      (dirtify)))

(defn delete-team [t team-id]
  (-> (teams t)
      (e/remove-entity team-id)
      (->> (set-teams t)
           (dirtify))))

(defn add-history [event t]
  (update t :history conj event))