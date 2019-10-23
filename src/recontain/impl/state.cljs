(ns recontain.impl.state
  (:require [reagent.core :as r]
            [clojure.string :as string]))

(defonce container-configurations (atom {}))
(defonce container-states-atom (atom {}))
(defonce components-configurations (atom {}))

(defonce recontain-settings-atom (atom {}))

(defonce reload-configuration-count (r/atom 0))
(defonce container-fn (atom nil))
(defonce component-fn (atom nil))

(def ^:dynamic *current-container* nil)
(def ^:dynamic *passed-values* nil)
(def ^:dynamic *current-handle* nil)

(defn reload-container-configurations []
  (swap! reload-configuration-count inc))

(defn setup [config {:keys [container-function component-function]}]
  (reset! container-fn container-function)
  (reset! component-fn component-function)

  (reset! container-states-atom {})
  (reset! recontain-settings-atom (assoc config :anonymous-count 0))
  (reset! components-configurations (:components config))
  (reset! container-configurations (reduce (fn [m v] (assoc m (:config-name v) v)) {} (:containers config)))
  @recontain-settings-atom)

(defn debug [f]
  (when (:debug? @recontain-settings-atom) (f)))

(defn get-container-config [container-name]
  (let [cfg (get @container-configurations container-name)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration for container: " container-name)))))
    cfg))

(defn get-component-config [component-id]
  (get @components-configurations component-id))


(defn get-container-data [container-id]
  (get @container-states-atom container-id))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn clear-container-state [h]
  (let [{:keys [handle-id local-state-path]} h]
    (swap! container-states-atom dissoc handle-id)
    (swap! (:state-atom @recontain-settings-atom) dissoc-path (drop-last local-state-path))))

(defn delete-local-state [h]
  (let [{:keys [handle-id local-state-path]} h]
    (swap! container-states-atom dissoc-path [handle-id :local-state])
    (swap! (:state-atom @recontain-settings-atom) dissoc-path local-state-path)))

(defn mk-container-id [ctx container-name]
  (let [ctx-id (->> (get-container-config container-name)
                    :ctx
                    (select-keys ctx)
                    hash)]
    (->> (str container-name "@" ctx-id))))

(defn id->str [id]
  (if (keyword? id) (name id) (str id)))

(defn dom-id [parent-id sub-id] (str (id->str parent-id) "#" (id->str sub-id)))

(defn validate-ctx [container-name given-ctx]
  (let [rq-ctx (:ctx (get-container-config container-name))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]
    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for container " container-name ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn dispatch [{:keys [handle dispatch-f args silently-fail?]}]
  (let [{:keys [config-name handle-id]} handle
        f (-> @container-configurations
              (get config-name)
              (get dispatch-f))]

    (if f
      (binding [*current-container* (update-in (get-container-data handle-id) [:local-state] merge *passed-values*)]
        (apply f handle args))
      (when-not silently-fail? (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in container-name " config-name)))))))

(defn get-handle [ctx container-name]
  (validate-ctx container-name ctx)
  (get-container-data (mk-container-id ctx container-name)))

(defn update! [state {:keys [handle-id local-state-path] :as opts} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-container-data handle-id))) (rest args)))]
    (update-in state local-state-path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @recontain-settings-atom) #(apply update! % handle args)))