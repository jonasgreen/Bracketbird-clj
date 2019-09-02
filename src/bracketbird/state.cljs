(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))


(defonce state (r/atom {:hooks        {}
                        :system       {}
                        :applications {}
                        :ui           {}}))


(def hooks-spc {:system               [:system]
                 :applications        [:applications]
                 :application         [:applications :application-id]
                 :tournament          ['application :tournament]

                 :teams               ['tournament :teams]
                 :teams-order         ['tournament :teams-order]
                 :team                ['tournament :teams :teams-id]

                 :stages              ['tournament :stages]
                 :stages-order        ['tournament :stages-order]
                 :stage               ['tournament :stages :stage-id]

                 ;notice these are matches in a given stage
                 :stage-matches       ['stage :matches]
                 :stage-matches-order ['stage :matches :matches-order]
                 :stage-match         ['stage :matches :match-id]

                 :groups              ['stage :groups]
                 :groups-order        ['stage :groups-order]
                 :group               ['stage :groups :group-id]

                 :group-matches       ['group :matches]
                 :group-matches-order ['group :matches :matches-order]
                 :group-match         ['group :matches :match-id]

                 ;; UI

                 :ui                  [:ui]
                 :system-panel        [:ui :ui-system]
                 :applications-panel  [:ui :ui-applications]
                 :application-panel   [:ui :ui-applications :application-id]

                 :front-page          ['application-panel :front-page]
                 :tournament-page     ['application-panel :tournament-page]})

(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [p (get hooks h)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (symbol? (first p))
      (into (resolve-path hooks (-> (first p) name keyword)) (vec (rest p)))
      p)))


(defn- mk-path [hooks h ctx]
  (->> (resolve-path hooks h)
       (map (fn [p] (get ctx p p)))
       (vec)))

(defn update-ui! [value]
  (let [p (conj (:_ui-path value) :_values)
        v (dissoc value :_ui-path)]

    (swap! state assoc-in p v)))

(defn hook-path [h ctx]
  (mk-path (:hooks @state) h ctx))

(defn hook
  ([h ctx]
   (hook h ctx {}))

  ([h ctx not-found]
   (let [path (hook-path h ctx)]
     (if (= :ui (first path))
       (reaction (-> @state
                     (get-in (conj path :_values) not-found)
                     (assoc :_ui-path path)))
       (reaction (get-in @state path not-found))))))


(defn dom-id [ctx k])

; ABOVE IS NEW

