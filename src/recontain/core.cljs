(ns recontain.core
  (:require [goog.dom :as dom]
            [clojure.string :as string]
            [recontain.impl.state :as rc-state]
            [recontain.impl.container :as rc-container]))



(declare ls put!)

(def elements {:change {:on-change (fn [this] (put! this assoc
                                                    (rc-state/sub-name (ls :element-name) "value")
                                                    (.. (ls :event) -target -value)))

                        :value     (fn [_] (ls (rc-state/sub-name (ls :element-name) "value")))}

               :hover  {:on-mouse-enter (fn [this] (put! this assoc (rc-state/sub-name (ls :element-name) "hover?") true))
                        :on-mouse-leave (fn [this] (put! this assoc (rc-state/sub-name (ls :element-name) "hover?") false))}


               :focus  {:on-focus (fn [this] (put! this assoc (rc-state/sub-name (ls :element-name) "focus?") true))
                        :on-blur  (fn [this] (put! this assoc (rc-state/sub-name (ls :element-name) "focus?") false))}


               :active {:on-mouse-down (fn [{:keys [element-name] :as this}] (put! this assoc (rc-state/sub-name element-name "active?") true))
                        :on-mouse-up   (fn [{:keys [element-name] :as this}] (put! this assoc (rc-state/sub-name element-name "active?") false))}

               :scroll {:on-scroll (fn [this] (let [t (.-target (ls :event))
                                                    scroll-top (.-scrollTop t)
                                                    scroll-height (.-scrollHeight t)
                                                    client-height (.-clientHeight t)]

                                                (rc-state/put! this assoc
                                                               (rc-state/sub-name (ls :element-name) "scroll-top") scroll-top
                                                               (rc-state/sub-name (ls :element-name) "scroll-height") scroll-height
                                                               (rc-state/sub-name (ls :element-name) "client-height") client-height
                                                               (rc-state/sub-name (ls :element-name) "scroll-bottom") (- scroll-height scroll-top client-height))))}
               })




(def event-bindings #_{:key {:on-key-down (fn [h sub-id ls e]
                                            (let [backspace? (= 8 (.-keyCode e))
                                                  delete? (get ls (rc-state/sub-name sub-id "delete-on-backspace?"))]
                                              (when (and backspace? delete?)
                                                (.stopPropagation e)
                                                #_(dispatch-silent h [sub-id :delete-on-backspace]))))
                             :on-key-up   (fn [h sub-id _ e]
                                            (when (= "text" (.-type (.-target e)))
                                              (rc-state/put! h assoc (rc-state/sub-name sub-id "delete-on-backspace?") (clojure.string/blank? (.. e -target -value)))))}})


(defn update! [state-m handle & args]
  (apply rc-state/update! state-m handle args))

(defn put! [handle & args] (apply rc-state/put! handle args))

(defn delete-local-state [handle] (rc-state/delete-local-state handle))

(defn dispatch [h f & args]

  (rc-state/dispatch {:handle h :dispatch-f f :args args :silently-fail? false}))

(defn call [h f & args]
  (binding [rc-state/*current-handle* h]
    (if-let [cf (get-in h [:raw-config f])]
      (cf h args)
      (throw (js/Error. (str "Function " f " is not defined in " h))))))

(defn get-dom-element [handle sub-id] (-> handle :handle-id (rc-state/dom-id sub-id) dom/getElement))

(defn get-handle ([ctx container-name] (rc-state/get-handle ctx container-name)))

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
       (dispatch 'focus)))

  ([handle container-name]
   (focus handle container-name {})))


(defn ls [& ks]
  ;TODO - also support lookup of local state of children like (rc/ls [::add-panel ::add-button] :button-hover?)
  (println "LS " ks (:stack-index rc-state/*current-handle*))

  (let [lsm (merge (:local-state rc-state/*current-handle*) (:element-state rc-state/*current-handle*))]
    (if (seq ks)
      (get-in lsm (if (vector? (first ks)) (first ks) (vec ks)))
      lsm)))

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states rc-state/*current-handle*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states rc-state/*current-handle*)))

(defn container
  ([ctx c]
   (container ctx c {}))

  ([ctx c optional-value]
   [rc-container/mk-container ctx c optional-value]))

(defn component [opts v]
  (rc-container/mk-component opts v))

(defn setup [config] (rc-state/setup config {:container-function container
                                             :component-function component
                                             :elements           elements}))

(defn reload-configurations [] (rc-state/reload-container-configurations))


