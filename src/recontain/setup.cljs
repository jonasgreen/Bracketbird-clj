(ns recontain.setup
  (:require [clojure.set :as set]))

(defn- ui-hook?
  ([hook-value] (map? hook-value))
  ([hooks h] (ui-hook? (get hooks h))))

(defn resolve-path [m k]
  (let [v (get m k)
        path (if (ui-hook? v) (:path v) v)]

    (if (get m (first path))                                ; parent ref
      (into (resolve-path m (first path)) (vec (rest path)))
      path)))

(defn resolve-ctx [path]
  (->> path
       (filter set?)
       (apply set/union)))

(defn resolve-path-and-ctx [hooks]
  (reduce (fn [m k]
            (let [path (resolve-path hooks k)
                  ctx (resolve-ctx path)]
              (assoc m k {:path path :ctx ctx}))) {} (keys hooks)))

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
  (let [{:keys [hooks]} config]
    (validate-hooks hooks)

    (let [ui-hooks (reduce-kv (fn [m k v] (if (ui-hook? v) (assoc m k v) m)) {} hooks)
          data-hooks (reduce-kv (fn [m k v] (if (not (ui-hook? v)) (assoc m k v) m)) {} hooks)

          data-hooks-path-ctx (resolve-path-and-ctx data-hooks)
          ui-hooks-path-ctx (resolve-path-and-ctx ui-hooks)

          ui-hooks-result (reduce-kv (fn [m k {:keys [path ctx]}]
                                       (update-in m [k] assoc :path path :ctx ctx)) ui-hooks ui-hooks-path-ctx)]

      (update config :hooks merge ui-hooks-result data-hooks-path-ctx))))
