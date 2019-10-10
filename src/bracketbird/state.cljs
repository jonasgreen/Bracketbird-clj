(ns bracketbird.state
  (:require [reagent.core :as r]))


(defonce state (r/atom {}))


(def hooks {:hook/system              [:system]
            :hook/applications        [:applications]
            :hook/application         [:applications #{:application-id}]
            :hook/tournament          [:hook/application :tournaments #{:tournament-id}]

            :hook/teams               [:hook/tournament :teams]
            :hook/teams-order         [:hook/tournament :teams-order]
            :hook/team                [:hook/tournament :teams #{:team-id}]

            :hook/stages              [:hook/tournament :stages]
            :hook/stages-order        [:hook/tournament :stages-order]
            :hook/stage               [:hook/tournament :stages #{:stage-id}]

            ;notice these are matches in a given stage
            :hook/stage-matches       [:hook/stage :matches]
            :hook/stage-matches-order [:hook/stage :matches :matches-order]
            :hook/stage-match         [:hook/stage :matches #{:match-id}]

            :hook/groups              [:hook/stage :groups]
            :hook/groups-order        [:hook/stage :groups-order]
            :hook/group               [:hook/stage :groups #{:group-id}]

            :hook/group-matches       [:hook/group :matches]
            :hook/group-matches-order [:hook/group :matches :matches-order]
            :hook/group-match         [:hook/group :matches #{:match-id}]})


(def resolved-hooks
  (letfn [(resolve [k]
            (let [path (get hooks k)]
              (if (and (< 1 (count path)) (get hooks (first path)))
                (into (resolve (first path)) (vec (rest path)))
                path)))]
    (reduce (fn [m k] (assoc m k (resolve k))) {} (keys hooks))))


(defn path [hook ctx]
  (reduce (fn [v p]
            (if (set? p)
              (if-let [ctx-value (get ctx (first p))]
                (conj v ctx-value)
                (throw (js/Error. (str "Missing context " p " for hook " hook ". Given ctx: " ctx))))
              (conj v p)))
          []
          (get resolved-hooks hook)))


(defn get-data [ctx hook]
  (->> (path hook ctx) (get-in @state)))

#_(defn generate-data-path [hook-value data-hooks]
    (let [p (:path hook-value)
          parent-hook-value (get data-hooks (first p))]

      (if parent-hook-value                                 ;; parent ref
        (into (generate-data-path parent-hook-value data-hooks) (vec (rest p)))
        p)))
