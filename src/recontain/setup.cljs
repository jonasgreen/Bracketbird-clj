(ns recontain.setup
  (:require [clojure.set :as set]))

; can be used before hook-type is set
(defn ui-hook? [hook-value]
  (= :ui (:hook-type hook-value)))

;obs - hooks are original unmodified
(defn generate-path [hook-value hooks]
  (let [p (:path hook-value)
        parent-hook (get hooks (first p))]

    (if parent-hook                                         ;; parent ref
      (into (generate-path parent-hook hooks) (vec (rest p)))
      p)))

(defn resolve-path [hook-value hooks]
  (assoc hook-value :path (generate-path hook-value hooks)))

(defn resolve-ctx [hook-value]
  (->> (:path hook-value)
       (filter set?)
       (apply set/union)
       (assoc hook-value :ctx)))

(defn resolve-hook [hooks hook-value]
  (-> hook-value
      (resolve-path hooks)
      resolve-ctx))


(defn ensure-structure-and-type [hook-value]
  (if (map? hook-value)
    (assoc hook-value :hook-type :ui)
    {:path hook-value :hook-type :data}))

(defn validate-hook [hook v]
  (cond
    (nil? v) (throw (js/Error. (str hook " is not defined in config")))
    (map? v) (:path v)
    (vector? v) v
    :else (throw (js/Error. (str "Wrong data type of hook " hook ". Allowed types are Vector (data-hook) and Map (ui-hook).")))))

(defn validate-hooks [hooks]
  (doall (->> hooks
              keys
              (map (fn [h] (validate-hook h (get hooks h)))))))

(defn resolve-config [config]
  (validate-hooks (:hooks config))

  ;first ensure structure - all hook values are maps
  (let [hooks (reduce-kv (fn [m k v] (assoc m k (ensure-structure-and-type v)))
                         {}
                         (:hooks config))]

    (reduce (fn [c h] (update-in c [:hooks h] (fn [_] (resolve-hook hooks (get hooks h)))))
            config
            (keys hooks))))
