(ns recontain.core
  (:require [goog.dom :as dom]
            [clojure.string :as string]
            [recontain.impl.state :as rc-state]
            [recontain.impl.container :as rc-container]
            [recontain.impl.config-stack :as rc-config-stack]))



(declare ls put!)

(defn sub-name [{:keys [rc-element-name]} sub-name]
  (keyword (str (name rc-element-name) "-" (name sub-name))))


(declare this)
(def elements {:change {:on-change (fn [data] (put! (sub-name data "value") (.. (:rc-event data) -target -value)))
                        :value     (fn [data] (ls (sub-name data "value")))}

               :hover  {:on-mouse-enter (fn [data] (put! (sub-name data "hover?") true))
                        :on-mouse-leave (fn [data] (put! (sub-name data "hover?") false))}

               :focus  {:on-focus (fn [data] (put! (sub-name data "focus?") true))
                        :on-blur  (fn [data] (put! (sub-name data "focus?") false))}


               :active {:on-mouse-down (fn [data] (put! (sub-name data "active?") true))
                        :on-mouse-up   (fn [data] (put! (sub-name data "active?") false))}

               :scroll {:on-scroll (fn [data] (let [t (.-target (:rc-event data))
                                                    scroll-top (.-scrollTop t)
                                                    scroll-height (.-scrollHeight t)
                                                    client-height (.-clientHeight t)]

                                                (put!
                                                      (sub-name data "scroll-top") scroll-top
                                                      (sub-name data "scroll-height") scroll-height
                                                      (sub-name data "client-height") client-height
                                                      (sub-name data "scroll-bottom") (- scroll-height scroll-top client-height))))}})




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

(defn put! [& args] (apply rc-state/put! (this) assoc args))

(defn delete-local-state [handle] (rc-state/delete-local-state handle))

(defn dispatch [h f & args]
  (rc-state/dispatch {:handle h :dispatch-f f :args args :silently-fail? false}))

(defn call [f & args]
  (if-let [cf (get-in (this) [:raw-config f])]
    (apply cf args)
    (throw (js/Error. (str "Function " f " is not defined in " (this))))))

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

  (let [lsm (:local-state rc-state/*current-handle*)]
    (if (seq ks)
      (get-in lsm (if (vector? (first ks)) (first ks) (vec ks)))
      lsm)))

(defn super [k]
  (let [{:keys [config-stack config-stack-index]} rc-state/*current-handle*
        super-fn (rc-config-stack/super config-stack config-stack-index k)

        ]

    )

  )

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states rc-state/*current-handle*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states rc-state/*current-handle*)))

(defn container
  ([ctx c]
   (container ctx c {}))

  ([ctx c optional-value]
   [rc-container/mk-container ctx c optional-value]))

(defn component [opts handle config-stack]
  (rc-container/mk-component opts handle config-stack))

(defn setup [config] (rc-state/setup config {:container-function container
                                             :component-function component
                                             :elements           elements}))

(defn reload-configurations [] (rc-state/reload-container-configurations))



