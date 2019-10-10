(ns recontain.impl.state
  (:require [reagent.core :as r]))

(defonce container-configurations (atom {}))
(defonce container-states-atom (atom {}))

(defonce recontain-settings-atom (atom {}))

(defonce reload-configuration-count (r/atom 0))
(defonce container-fn (atom nil))

(def ^:dynamic *current-container* nil)
(def ^:dynamic *passed-values* nil)

(defn reload-container-configurations []
  (swap! reload-configuration-count inc))

(defn setup [config container-function]
  (reset! container-fn container-function)
  (reset! container-states-atom {})
  (reset! recontain-settings-atom (assoc config :anonymous-count 0))
  (reset! container-configurations (reduce (fn [m v] (assoc m (:hook v) v)) {} (:containers config)))
  @recontain-settings-atom)

(defn debug [f]
  (when (:debug? @recontain-settings-atom) (f)))

(defn get-container-config [hook]
  (let [cfg (get @container-configurations hook)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration for container: " hook)))))
    cfg))

(defn get-container-data [id] (get @container-states-atom id))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn clear-container-state [h]
  (let [{:keys [id path]} h]
    (swap! container-states-atom dissoc id)
    (swap! (:state-atom @recontain-settings-atom) dissoc-path (drop-last path))))

(defn delete-local-state [h]
  (let [{:keys [id path]} h]
    (swap! container-states-atom dissoc-path [id :local-state])
    (swap! (:state-atom @recontain-settings-atom) dissoc-path path)))

(defn mk-container-id [ctx hook]
  (let [ctx-id (->> (get-container-config hook)
                    :ctx
                    (select-keys ctx)
                    hash)]
    (->> (str hook "@" ctx-id))))

(defn dom-element-id [handle sub-id] (str (:id handle) "#" (if (keyword? sub-id) (name sub-id) sub-id)))

(defn mk-handle [parent-handle ctx {:keys [id path hook all-paths]}]
  {:parent-handle-id (:id parent-handle)
   :id               id
   :ctx              ctx
   :path             path
   :hook             hook
   :foreign-paths    (-> all-paths
                         (dissoc hook)
                         vals
                         vec)})

(defn validate-ctx [hook given-ctx]
  (let [rq-ctx (:ctx (get-container-config hook))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]
    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for container " hook ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn dispatch [{:keys [handle dispatch-f args silently-fail?]}]
  (let [{:keys [hook id]} handle
        f (-> @container-configurations
              (get hook)
              (get dispatch-f))]

    (if f
      (binding [*current-container* (update-in (get-container-data id) [:local-state] merge *passed-values*)]
        (apply f handle args))
      (when-not silently-fail? (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in hook " hook)))))))

(defn get-handle [ctx hook]
  (validate-ctx hook ctx)
  (:handle (get-container-data (mk-container-id ctx hook))))

(defn update! [state {:keys [id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-container-data id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @recontain-settings-atom) #(apply update! % handle args)))