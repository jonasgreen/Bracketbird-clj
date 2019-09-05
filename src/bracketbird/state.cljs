(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))


(defonce state (r/atom {}))

(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [p (get hooks h)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn update-ui! [value]
  (let [p (conj (:_ui-path value) :_values)
        v (dissoc value :_ui-path)]
    (swap! state assoc-in p v)))

(defn put!
  ([ui-values k v]
   (-> ui-values (assoc k v) update-ui!))
  ([ui-values k v & kvs]
   (-> (apply assoc ui-values k v kvs) update-ui!)))

(defn hook-path [h ctx]
  (->> (resolve-path (:hooks @state) h)
       ;replace id's
       (map (fn [p] (get ctx p p)))
       (vec)))


(defn- subscribe-ui
  ([path]
   (subscribe-ui path nil))
  ([path not-found]
   (reaction (-> @state
                 (get-in (conj path :_values) (if not-found not-found {}))
                 (assoc :_ui-path path)))))

(defn subscribe
  ([path] (subscribe path nil))
  ([path not-found] (reaction (get-in @state path not-found))))

(defn hook
  ([h ctx]
   (hook h ctx nil))
  ([h ctx not-found]
   (let [path (hook-path h ctx)]
     (if (= :ui (first path))
       (subscribe-ui path not-found)
       (subscribe path not-found)))))


(defn dom-id [ctx k])

; ABOVE IS NEW

