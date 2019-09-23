(ns recontain.setup
  (:require [clojure.set :as set]))

; can be used before hook-type is set
(defn ui-hook? [hook-value]
  (= :ui (:hook-type hook-value)))

;obs - hooks are original unmodified
(defn generate-data-path [hook-value data-hooks]
  (let [p (:path hook-value)
        parent-hook (get data-hooks (first p))]

    (if parent-hook                                         ;; parent ref
      (into (generate-data-path parent-hook data-hooks) (vec (rest p)))
      p)))

(defn resolve-data-path [hook-value hooks]
  (assoc hook-value :path (generate-data-path hook-value hooks)))

(defn resolve-ctx [hook-value]
  (->> (:path hook-value)
       (filter set?)
       (apply set/union)
       (assoc hook-value :ctx)))


(defn resolve-data-hooks [data-hooks]
  (let [m-hooks (reduce-kv (fn [m k v] (assoc m k {:path v :hook-type :data})) {} data-hooks)]
    (reduce (fn [m k] (update m k (-> %
                                      (resolve-data-path data-hooks)
                                      resolve-ctx)))
            m-hooks
            (keys m-hooks))))


#_[ui-root
   [ui-application-page #{:application-id}
    [ui-front-page]
    [ui-tournament-page #{:tournament-id}
     [ui-teams-tab
      [ui-team-row #{:team-id}]
      [ui-enter-team-input]]
     [ui-settings-tab]
     [ui-matches-tab]
     [ui-ranking-tab]]]]

(defn resolve-ui-hooks [data-hooks ui-layout]
  (loop [{:keys [hook] :as component} (first ui-layout)
         parent nil
         resolved-hooks {}]

    (let [children (rest ui-layout)
          ctx (when (set? (first children)) (first children))
          rs-children (if ctx
                        (rest children)
                        children)

          path (-> (:path parent)
                   (conj  (if ctx
                            (sequence [hook (first children)])
                            hook))
                   vec)
          result {hook (assoc comp :parent parent
                                   :path path
                                   :ctx (merge (:ctx parent) ctx))}
          ]
      (if-not (seq rs-children)
        (merge result resolved-hooks)
        (merge (-> rs-children)
        )


      )




    )
  )

(defn validate-data-hook [data-hook v]
  (cond
    (nil? v) (throw (js/Error. (str data-hook " is not defined in config")))
    (vector? v) v
    :else (throw (js/Error. (str "Wrong value type of data-hook " data-hook ". Should be a Vector")))))

(defn validate-data-hooks [data-hooks]
  (doall (->> data-hooks
              keys
              (map (fn [h] (validate-data-hook h (get data-hooks h)))))))




(defn resolve-config [{:keys [data-hooks ui-layout] :as config}]
  (validate-data-hooks data-hooks)

  ;convert data hooks to maps
  (-> config
      (update :assoc :hooks (-> data-hooks
                                resolve-data-hooks
                                (resolve-ui-hooks ui-layout)))

      (dissoc :data-hooks :ui-layout)))


