(ns recontain.core
  (:require [goog.dom :as dom]
            [clojure.string :as string]
            [recontain.impl.state :as rc-state]
            [recontain.impl.element :as rc-element]
            [recontain.impl.container :as rc-container]))


(defn bind-options [opts] (rc-container/external-bind-options opts))

(defn update! [state-m handle & args] (apply rc-state/update! state-m handle args))

(defn put! [handle & args] (apply rc-state/put! handle args))

(defn delete-local-state [handle] (rc-state/delete-local-state handle))

(defn dispatch [h f & args] (rc-state/dispatch {:handle h :dispatch-f f :args args :silently-fail? false}))

(defn get-dom-element [handle sub-id] (-> handle :container-id (rc-state/dom-element-id sub-id) dom/getElement))

(defn get-handle [ctx container-name] (rc-state/get-handle ctx container-name))

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
    (get-in (:local-state rc-state/*current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:local-state rc-state/*current-container*)))

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states rc-state/*current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states rc-state/*current-container*)))

(defn container
  ([ctx c]
   (container ctx c {}))

  ([ctx c optional-value]
   [rc-container/mk-container ctx c optional-value]))

(defn element [opts v]
  (rc-element/mk-element opts v))

(defn setup [config] (rc-state/setup config {:container-function container :element-function element}))

(defn reload-configurations [] (rc-state/reload-container-configurations))
