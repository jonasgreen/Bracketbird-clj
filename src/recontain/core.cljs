(ns recontain.core
  (:require [goog.dom :as dom]
            [clojure.string :as string]
            [recontain.impl.state :as rc-state]
            [recontain.impl.container :as rc-container]
            [recontain.impl.config-stack :as rc-config-stack]))



(declare ls put!)

(defn sub-name [{:keys [rc-name]} sub-name]
  (keyword (str (name rc-name) "-" (name sub-name))))


(declare this)


(def event-bindings #_{:key {:on-key-down (fn [h sub-id ls e]
                                            (let [backspace? (= 8 (.-keyCode e))
                                                  delete? (get ls (sub-name "delete-on-backspace?"))]
                                              (when (and backspace? delete?)
                                                (.stopPropagation e)
                                                #_(dispatch-silent h [sub-id :delete-on-backspace]))))
                             :on-key-up   (fn [h sub-id _ e]
                                            (when (= "text" (.-type (.-target e)))
                                              (rc-state/put! h (sub-name "delete-on-backspace?") (clojure.string/blank? (.. e -target -value)))))}})


(defn this [& ks]
  (let [h rc-state/*current-handle*]
    (if (seq ks)
      (get-in h (if (vector? (first ks)) (first ks) (vec ks)))
      h)))

(defn update! [state-m handle & args]
  (apply rc-state/update! state-m handle args))

(defn put! [& args]
  "Implicit assoc"
  (apply rc-state/put! (this) assoc args))

(defn delete-local-state [handle] (rc-state/delete-local-state handle))

(defn dispatch [{:keys [raw-config-stack config-name] :as handle} f-key & args]
  (if-let [f (rc-config-stack/config-value raw-config-stack f-key)]
    (apply f nil args)
    (throw (js/Error. (str "Dispatch function " f-key " is not defined in config " config-name)))))

(defn super [k & opts]
  (let [index (-> @(:config-stack rc-state/*current-handle*) :configs count)
        {:keys [value _]} (rc-config-stack/config-value rc-state/*execution-stack* k index)]

    (if (fn? value)
      (apply value opts)
      value)))

(defn call [k & args]
  (let [{:keys [value _]} (-> @(:config-stack rc-state/*current-handle*) (rc-config-stack/config-value k))]

    (when-not value (throw (js/Error. (str "Function " k " not found in config-stack"))))
    (when-not (fn? value) (throw (js/Error. (str k "is not a function"))))

    (apply value "whaaat" args)))

(defn dom-element
  ([sub-id]
   (dom-element (this) sub-id))

  ([handle sub-id]
   (-> handle :handle-id (rc-state/dom-id sub-id) dom/getElement)))

(defn focus-dom-element
  ([sub-id]
   (focus-dom-element (this) sub-id))

  ([handle sub-id]
   (-> (dom-element handle sub-id) (.focus))))


(defn component-handle [element-ref]
  (-> (this)
      :handle-id
      (rc-state/dom-id element-ref)
      rc-state/get-handle))

(defn container-handle ([ctx container-name] (rc-state/get-container-handle ctx container-name)))

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
       (container-handle container-name)
       (dispatch 'focus)))

  ([handle container-name]
   (focus handle container-name {})))



(defn ls
  "Local state of children-components can be accessed in this way (ls ::child-a ::child-of-child-a :button-hover?)"
  ([]
   (ls (this)))

  ([k-or-handle]
   (if (keyword? k-or-handle)
     (ls (this) k-or-handle)
     (ls k-or-handle nil)))

  ([handle k]
   (let [h (if (keyword? handle) (component-handle handle) handle)
         lsm (:local-state h)]
     (if k (get lsm k) lsm))))

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states rc-state/*current-handle*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states rc-state/*current-handle*)))

(defn container
  ([c-name]
   (container c-name))

  ([c-name m]
   ;;will be parsed and made fit [rc-container/mk-container data] like root calls it
   [rc-container/mk-container c-name m]))

(defn root [root-config-name]
  [rc-container/mk-container {:rc-type         root-config-name
                              :rc-component-id (rc-state/mk-container-id {} root-config-name)}])



(defn setup [config] (rc-state/setup config {:container-function container}))

(defn reload-configurations [] (rc-state/reload-container-configurations))



