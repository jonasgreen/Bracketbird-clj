(ns recontain.impl.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))

(defonce container-configurations (atom {}))
(defonce handles-atom (atom {}))

;; decorations and components and elements - later also containers
(defonce configurations (r/atom {}))

(defonce recontain-settings-atom (atom {}))

(defonce reload-configuration-count (r/atom 0))
(defonce container-fn (atom nil))

(def ^:dynamic *current-container-handle* nil)
(def ^:dynamic *passed-values* nil)
(def ^:dynamic *current-handle* nil)
(def ^:dynamic *execution-stack* nil)


(defn reload-container-configurations []
  (swap! reload-configuration-count inc))

(defn setup [{:keys [decorations components elements] :as config} {:keys [container-function]}]
  (reset! container-fn container-function)

  (reset! handles-atom {})
  (reset! recontain-settings-atom (assoc config :anonymous-count 0))
  (reset! container-configurations (reduce (fn [m v] (assoc m (:config-name v) v)) {} (:containers config)))
  ;TODO validate no conflicts
  (reset! configurations (merge decorations components elements))

  @recontain-settings-atom)

(defn debug [f]
  (when (:debug? @recontain-settings-atom) (f)))

(defn get-container-config [container-name]
  (let [cfg (get @container-configurations container-name)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration for container: " container-name)))))
    cfg))

(defn get-config [config-name]
  (let [cfg (get @configurations config-name)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration found for: " config-name)))))
    cfg))

(defn get-config-with-inherits [config-name]
  (loop [cfg-name config-name configs []]
    (if-not cfg-name
      configs
      (let [cfg (get-config cfg-name)]
        (recur (:inherits cfg) (conj configs [cfg-name cfg]))))))

(defn get-handle [handle-id]
  (get @handles-atom handle-id))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn clear-container-state [h]
  (let [{:keys [handle-id local-state-path]} h]
    (swap! handles-atom dissoc handle-id)
    (swap! (:state-atom @recontain-settings-atom) dissoc-path (drop-last local-state-path))))

(defn delete-local-state [h]
  (let [{:keys [handle-id local-state-path]} h]
    (swap! handles-atom dissoc-path [handle-id :local-state])
    (swap! (:state-atom @recontain-settings-atom) dissoc-path local-state-path)))

(defn mk-container-id [ctx container-name]
  (let [ctx-id (->> (get-container-config container-name)
                    :ctx
                    (select-keys ctx)
                    hash)]
    (->> (str container-name "@" ctx-id))))

(defn id->str [id]
  (if (keyword? id) (name id) (str id)))

(defn dom-id [parent-id & sub-ids]
  (->> sub-ids
       (remove nil?)
       (reduce (fn [r s] (str r "#" (id->str s))) (id->str parent-id))))

(defn validate-ctx [container-name given-ctx]
  (let [rq-ctx (:ctx (get-container-config container-name))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]
    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for container " container-name ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn dispatch [{:keys [handle dispatch-f args]}]
  (let [{:keys [config-name handle-id]} handle
        f (-> @container-configurations
              (get config-name)
              (get dispatch-f))]

    (if f
      (binding [*current-container-handle* (update-in (get-handle handle-id) [:local-state] merge *passed-values*)]
        (apply f handle args))
      (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in container-name " config-name))))))

(defn get-container-handle [ctx container-name]
  (validate-ctx container-name ctx)
  (get-handle (mk-container-id ctx container-name)))

(defn update! [state {:keys [handle-id local-state-path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-handle handle-id))) (rest args)))]
    (update-in state local-state-path upd)))

(defn remove! [state {:keys [local-state-path]}]
  (let [remove-key (-> local-state-path drop-last last)
        path (->> local-state-path (drop-last 2) vec)]
    (update-in state path dissoc remove-key)))


(defn put! [handle & args]
  (swap! (:state-atom @recontain-settings-atom) #(apply update! % handle args)))