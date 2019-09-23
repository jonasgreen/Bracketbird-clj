(ns recontain.setup
  (:require [clojure.set :as set]))

; can be used before hook-type is set
(defn ui-hook? [hook-value]
  (= :ui (:hook-type hook-value)))

;obs - hooks are original unmodified
(defn generate-data-path [hook-value data-hooks]
  (let [p (:path hook-value)
        parent-hook-value (get data-hooks (first p))]

    (if parent-hook-value                                         ;; parent ref
      (into (generate-data-path parent-hook-value data-hooks) (vec (rest p)))
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
    (reduce (fn [m k] (update m k #(-> %
                                       (resolve-data-path m-hooks)
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


(defn resolve-ui-hook [a parent v]
  (let [{:keys [hook] :as com} (first v)
        children (next v)
        ctx-set (when (set? (first children))
                  (first children))

        comp-children (if ctx-set (next children) children)

        container (assoc com
                    :hook-type :ui
                    :ctx (set/union (:ctx parent) ctx-set)
                    :path (-> (get parent :path [])
                              (conj hook)
                              (into (if ctx-set [ctx-set] []))
                              vec)
                    :parent (:hook parent))]

    (swap! a update hook (fn [v]
                           (if-not v
                             container
                             (throw (js/Error. (str "Recontain config error. Multiple occurrences of " hook))))))

    (doall (map (partial resolve-ui-hook a container) comp-children))))

(defn resolve-ui-hooks [data-hooks ui-layout]
  (let [a (atom {})]
    (resolve-ui-hook a nil ui-layout)
    @a))

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
  (let [r-data-hooks (resolve-data-hooks data-hooks)
        r-ui-hooks (resolve-ui-hooks r-data-hooks ui-layout)
        render-to-hook (reduce-kv (fn [m k v] (assoc m (:render v) k)) {} r-ui-hooks)]
    (-> config
        (assoc :hooks (merge r-data-hooks r-ui-hooks)
               :render-to-hook render-to-hook)

        (dissoc :data-hooks :ui-layout)


        )

    ))


