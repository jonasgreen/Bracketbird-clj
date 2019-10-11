(ns recontain.core
  (:require [goog.dom :as dom]
            [clojure.string :as string]
            [recontain.impl.state :as state]
            [recontain.impl.container :as con]))


(defn bind-options [opts] (con/external-bind-options opts))

(defn update! [state-m handle & args] (apply state/update! state-m handle args))

(defn put! [handle & args] (apply state/put! handle args))

(defn delete-local-state [handle] (state/delete-local-state handle))

(defn dispatch [h f & args] (state/dispatch {:handle h :dispatch-f f :args args :silently-fail? false}))

(defn get-dom-element [handle sub-id] (-> handle (state/dom-element-id sub-id) dom/getElement))

(defn get-handle [ctx container-name] (state/get-handle ctx container-name))

(defn has-changed [value org-value]
  (when value
    (if (and (string? value) (string? org-value))
      (if (and (string/blank? value)
               (string/blank? org-value))
        false
        (not= value org-value))
      (not= value org-value))))

(defn focus
  ([handle container-name ctx-id ctx-value]
   (when-let [ctx-value (if (map? ctx-value) (get ctx-value ctx-id) ctx-value)]
     (focus handle container-name {ctx-id ctx-value})))

  ([handle container-name extra-ctx]
   (-> (:ctx handle)
       (merge extra-ctx)
       (get-handle container-name)
       (dispatch :focus)))

  ([handle container-name]
   (focus handle container-name {})))


(defn ls [& ks]
  (if (seq ks)
    (get-in (:local-state state/*current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:local-state state/*current-container*)))

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states state/*current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states state/*current-container*)))

(defn container
  ([ctx c]
   (container ctx c {}))

  ([ctx c optional-value]
   [con/mk-container ctx c optional-value]))

(defn setup [config] (state/setup config container))

(defn reload-configurations [] (state/reload-container-configurations))
