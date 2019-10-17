(ns recontain.impl.state
  (:require [reagent.core :as r]))

(defonce container-configurations (atom {}))
(defonce container-states-atom (atom {}))
(defonce elements-configurations (atom {}))

(defonce recontain-settings-atom (atom {}))

(defonce reload-configuration-count (r/atom 0))
(defonce container-fn (atom nil))
(defonce element-fn (atom nil))

(def ^:dynamic *current-container* nil)
(def ^:dynamic *passed-values* nil)

(defn reload-container-configurations []
  (swap! reload-configuration-count inc))

(defn setup [config {:keys [container-function element-function]}]
  (reset! container-fn container-function)
  (reset! element-fn element-function)

  (reset! container-states-atom {})
  (reset! recontain-settings-atom (assoc config :anonymous-count 0))
  (reset! elements-configurations (:elements config))
  (reset! container-configurations (reduce (fn [m v] (assoc m (:container-name v) v)) {} (:containers config)))
  @recontain-settings-atom)

(defn debug [f]
  (when (:debug? @recontain-settings-atom) (f)))

(defn get-container-config [container-name]
  (let [cfg (get @container-configurations container-name)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration for container: " container-name)))))
    cfg))

(defn get-container-data [container-id] (get @container-states-atom container-id))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn clear-container-state [h]
  (let [{:keys [container-id path]} h]
    (swap! container-states-atom dissoc container-id)
    (swap! (:state-atom @recontain-settings-atom) dissoc-path (drop-last path))))

(defn delete-local-state [h]
  (let [{:keys [container-id path]} h]
    (swap! container-states-atom dissoc-path [container-id :local-state])
    (swap! (:state-atom @recontain-settings-atom) dissoc-path path)))

(defn mk-container-id [ctx container-name]
  (let [ctx-id (->> (get-container-config container-name)
                    :ctx
                    (select-keys ctx)
                    hash)]
    (->> (str container-name "@" ctx-id))))

(defn id->str [id]
  (if (keyword? id) (name id) (str id)))

(defn dom-element-id [parent-id sub-id] (str (id->str parent-id) "#" (id->str sub-id)))

(defn mk-handle [parent-handle ctx {:keys [container-id path container-name all-paths]}]
  {:parent-handle-id (:container-id parent-handle)
   :container-id     container-id
   :ctx              ctx
   :path             path
   :container-name   container-name
   :foreign-paths    (-> all-paths
                         (dissoc container-name)
                         vals
                         vec)})

(defn validate-ctx [container-name given-ctx]
  (let [rq-ctx (:ctx (get-container-config container-name))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]
    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for container " container-name ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn dispatch [{:keys [handle dispatch-f args silently-fail?]}]
  (let [{:keys [container-name container-id]} handle
        f (-> @container-configurations
              (get container-name)
              (get dispatch-f))]

    (if f
      (binding [*current-container* (update-in (get-container-data container-id) [:local-state] merge *passed-values*)]
        (apply f handle args))
      (when-not silently-fail? (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in container-name " container-name)))))))

(defn get-handle [ctx container-name]
  (validate-ctx container-name ctx)
  (:handle (get-container-data (mk-container-id ctx container-name))))

(defn update! [state {:keys [container-id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-container-data container-id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @recontain-settings-atom) #(apply update! % handle args)))