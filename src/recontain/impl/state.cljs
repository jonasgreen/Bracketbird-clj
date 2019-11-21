(ns recontain.impl.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))

(defonce components-state-cache* (atom {}))
(defonce configurations* (r/atom {}))
(defonce recontain-settings* (atom {}))
(defonce reload-configuration-count* (r/atom 0))
(defonce container-fn* (atom nil))

(def ^:dynamic *passed-values* nil)
(def ^:dynamic *current-handle* nil)
(def ^:dynamic *execution-stack* nil)


(defn reload-container-configurations []
  (swap! reload-configuration-count* inc))

(defn setup [{:keys [decorations elements] :as config} {:keys [container-function]}]
  (reset! container-fn* container-function)

  (reset! components-state-cache* {})

  (reset! recontain-settings* (assoc config :anonymous-count 0))
  (reset! configurations* (merge (reduce (fn [m v] (assoc m (:config-name v) v)) {} (:components config))
                                 decorations
                                 elements))

  @recontain-settings*)

(defn debug [f]
  (when (:debug? @recontain-settings*) (f)))

(defn get-config [config-name]
  (let [cfg (get @configurations* config-name)]
    (when-not cfg
      (do
        (throw (js/Error. (str "No configuration found for: " config-name)))))
    cfg))

(defn component-state-cache [component-id]
  (get @components-state-cache* component-id))

(defn get-config-with-inherits [config-name]
  (loop [cfg-name config-name configs []]
    (if-not cfg-name
      configs
      (let [cfg (get-config cfg-name)]
        (recur (:inherits cfg) (conj configs [cfg-name cfg]))))))

(defn get-handle [handle-id]
  (get @components-state-cache* handle-id))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn delete-local-state [h]
  (let [{:keys [handle-id local-state-path]} h]
    (swap! components-state-cache* dissoc-path [handle-id :local-state])
    (swap! (:state-atom @recontain-settings*) dissoc-path local-state-path)))

(defn mk-container-id [ctx container-name]
  (let [ctx-id (->> (get-config container-name)
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
  (let [rq-ctx (:ctx (get-config container-name))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]
    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for container " container-name ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn dispatch [{:keys [handle dispatch-f args]}]
  ;TODO
  #_(let [{:keys [config-name handle-id]} handle
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
  (swap! (:state-atom @recontain-settings*) #(apply update! % handle args)))