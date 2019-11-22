(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [stateless.util :as ut]
            [recontain.impl.state :as rc-state]
            [recontain.impl.config-stack :as rc-config-stack]))

(def stack-atom (atom nil))

(declare mk-container mk-component)

(defn- namespaced?
  ([item]
   (and (keyword? item) (namespace item)))

  ([item str-namespace]
   (and (keyword? item)
        (= str-namespace (namespace item)))))


(defn event-key? [k]
  (string/starts-with? (if (sequential? k) (name (last k)) (name k)) "on-"))

(defn- options-value [config-stack data k]
  (let [{:keys [value index]} (rc-config-stack/config-value config-stack k)]
    (if (fn? value)
      (if (event-key? k)
        ;wrap events for later execution
        (fn [e]
          (binding [rc-state/*execution-stack* config-stack]
            (value (assoc data :rc-event e))))
        (binding [rc-state/*execution-stack* config-stack]
          (value data)))
      value)))


(defn- mk-element
  "Returns a vector containing the element and the elements options"
  [data handle config-stack]
  (let [{:keys [rc-type rc-dom-id]} data

        ;;Add decorate configs to stack - TODO should be able to overwrite decorations somehow

        config-stack (if (namespaced? rc-type "e")
                       (->> (rc-state/get-config-with-inherits (keyword (name rc-type)))
                            (reduce (fn [stack [c-name config]] (rc-config-stack/add-config stack handle c-name config)) config-stack))
                       config-stack)

        config-stack (->> (rc-config-stack/config-value config-stack :decorate)
                          :value
                          (reduce (fn [stack cfg] (->> cfg
                                                       rc-state/get-config
                                                       (rc-config-stack/add-config stack handle cfg)))
                                  config-stack))

        ;assemble options
        options (reduce (fn [m k]
                          (if (or (symbol? k) (= :render k) (= :decorate k))
                            m
                            (assoc m k (options-value config-stack data k))))
                        {} (rc-config-stack/config-keys config-stack))]

    (if (namespaced? rc-type "e")
      (ut/insert (assoc options :id rc-dom-id) 1 (options-value config-stack data :render))
      [rc-type (-> options
                   (assoc :id rc-dom-id))])))



(defn- container-form? [form]
  (and (vector? form)
       (= (first form) @rc-state/container-fn*)))

(defn- component-form? [form]
  (and (vector? form)
       (namespaced? (first form))
       (namespaced? (second form) "c")))

(defn- element-form? [form]
  (and
    (vector? form)
    (namespaced? (first form))
    (or (not (namespaced? (second form)))
        (namespaced? (second form) "e"))))

(defn- extract-passed-in-data [rest-of-form]
  (when-some [data (first rest-of-form)]
    (when (map? data)
      data)))

;;for the moment only one additional ctx is supported
(defn- extract-ctx [form handle]
  (let [additional-ctx (-> (meta form) (dissoc :key))]
    {:ctx          (merge (:ctx handle) additional-ctx)
     :meta-content (if (seq additional-ctx) {:key (get (first additional-ctx) 1)} (meta form))}))

(defn decorate-hiccup [form opts]
  (let [{:keys [component-data handle config-stack xs-data]} opts]
    (cond

      ;;container
      (container-form? form)
      (let [rc-type (second form)]

        ;additional context is found in meta and used as key if present
        (let [{:keys [ctx meta-content]} (extract-ctx form handle)
              passed-in-data (extract-passed-in-data (nnext form))
              rc-data {:rc-name         nil
                       :rc-type         rc-type
                       :rc-key          (:key meta-content)
                       :rc-ctx          ctx
                       :rc-component-id (rc-state/mk-container-id ctx rc-type)}]

          (rc-state/validate-ctx rc-type ctx)
          (-> (into [mk-component (merge passed-in-data rc-data (:xs-data opts)) handle rc-state/shadow-configuration*] (subvec form (if passed-in-data 3 2)))
              (with-meta meta-content))))


      ;;component
      (component-form? form)
      (let [{:keys [ctx meta-content]} (extract-ctx form handle)
            rc-name (keyword (name (first form)))
            rc-type (keyword (name (second form)))          ;remove namespace
            rc-key (:key meta-content)
            rc-component-id (rc-state/dom-id (:handle-id handle) rc-name rc-key)

            rest-of-form (nnext form)
            passed-in-data (extract-passed-in-data rest-of-form)

            rc-data {:rc-name         rc-name
                     :rc-type         rc-type
                     :rc-key          rc-key
                     :rc-ctx          ctx
                     :rc-component-id rc-component-id}
            ]

        (swap! stack-atom assoc rc-component-id {:config-stack config-stack})
        (-> (into [mk-component (merge passed-in-data rc-data (:xs-data opts)) handle rc-state/shadow-configuration*]
                  (subvec form (if passed-in-data 3 2)))
            (with-meta meta-content)))


      ;;vector of special form with ::keyword
      (element-form? form)
      (let [{:keys [ctx meta-content]} (extract-ctx form handle)
            rc-name (keyword (name (first form)))
            rc-type (if (keyword? (second form)) (second form) :div)

            rest-of-form (if (keyword? (second form))
                           (nnext form)
                           (next form))

            passed-in-data (extract-passed-in-data rest-of-form)

            rc-data {:rc-type   rc-type
                     :rc-name   rc-name
                     :rc-key    (:key meta-content)
                     :rc-ctx    ctx
                     :rc-dom-id (rc-state/dom-id (:handle-id handle) rc-name)}

            ;index-children (filter (or vector? string?) form)

            children (->> (if passed-in-data (next rest-of-form) rest-of-form)
                          (map (fn [f] (decorate-hiccup f (dissoc opts :xs-data))))
                          vec)]

        (-> (merge passed-in-data component-data rc-data (:xs-data opts))
            (mk-element handle (rc-config-stack/shave-by-element config-stack rc-name))
            (into children)
            ;preserve meta
            (with-meta meta-content)))


      (vector? form)
      (let [v (->> form (map (fn [f] (decorate-hiccup f opts))) vec)]
        ;preserve meta
        (with-meta v (meta form)))

      (sequential? form)
      (let [form-count (count form)]
        (->> form (map-indexed (fn [i f] (decorate-hiccup f (assoc opts :xs-data {:rc-index  i
                                                                                  :rc-last?  (= (inc i) form-count)
                                                                                  :rc-first? (= i 0)}))))))

      :else form)))


;only used to indicate a container (found in hiccup-decoration) will eventually result in a call to mk-component
(defn mk-container [_ _])

(defn mk-component [data parent-handle configurations*]

  (let [{:keys [rc-name rc-type rc-component-id rc-key rc-ctx]} data
        {:keys [config-stack]} (get @stack-atom rc-component-id)
        _ (println "MK" rc-type)

        state-atom* (get @rc-state/recontain-settings* :state-atom)
        config* (reaction (rc-state/get-config rc-type @configurations*))

        path-name (or rc-name rc-type)                      ; containers have no rc-name
        local-state-path (if parent-handle
                           (-> (:local-state-path parent-handle) drop-last vec (#(if rc-key
                                                                                   (conj % path-name rc-key :_local-state)
                                                                                   (conj % path-name :_local-state))))
                           [path-name :_local-state])
        foreign-state-paths (if-let [f (:foreign-state @config*)] (f data) {})

        local-state* (reaction (get-in @state-atom* local-state-path))
        foreign-states* (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom* v)))) {} foreign-state-paths)

        ;_ (rc-config-stack/print-config-stack config-stack)

        ;containers create a new stack
        config-stack (if rc-name (rc-config-stack/shave-by-component config-stack rc-name) (rc-config-stack/mk))
        raw-configs* (reaction (rc-state/get-config-with-inherits rc-type @configurations*))

        handle {:handle-id        rc-component-id
                :handle-type      :component
                :parent-handle-id (:handle-id parent-handle)
                :local-state-path local-state-path
                :config-name      rc-type
                :ctx              rc-ctx}
        ]

    (r/create-class
      {:component-will-unmount (fn [_] (swap! stack-atom dissoc rc-component-id))

       :reagent-render         (fn [input-data _ configs]
                                 (let [foreign-states (reduce-kv (fn [m k v] (assoc m k (deref v))) {} foreign-states*)

                                       ;initialize local state with foreign states
                                       local-state (or @local-state* (if-let [f (:local-state @config*)] (f (merge input-data foreign-states))) {})

                                       ;local-state

                                       ;  _ (println "raw config" raw-configs)
                                       new-config-stack (->> @raw-configs*
                                                             (reduce
                                                               (fn [stack [c-name config]] (rc-config-stack/add-config stack handle c-name config))
                                                               config-stack))

                                       _ (swap! rc-state/components-state-cache* assoc rc-component-id
                                                (merge handle {:local-state      local-state
                                                               :foreign-states   foreign-states
                                                               :raw-config-stack new-config-stack}))

                                       ;_ (rc-config-stack/print-config-stack new-config-stack)
                                       rendered (options-value new-config-stack input-data :render)]


                                   (if-not rendered
                                     [:div (str "No render: " rc-type "(" rc-name ")")]

                                     ;instead of reagent calling render function - we do it
                                     (let [result
                                           (-> rendered
                                               (decorate-hiccup {:component-data input-data
                                                                 :handle         handle
                                                                 :config-stack   new-config-stack}))]
                                       result
                                       ))))})))