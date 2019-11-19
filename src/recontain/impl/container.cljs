(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [reagent.core :as r]
            [stateless.util :as ut]
            [recontain.impl.state :as rc-state]
            [recontain.impl.config-stack :as rc-config-stack]
            [bracketbird.state :as state]))


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
       (= (first form) @rc-state/container-fn)))

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
              rc-data {:rc-name             nil
                       :rc-type             rc-type
                       :rc-key              (:key meta-content)
                       :rc-ctx              ctx
                       :rc-parent-handle-id (:handle-id handle)
                       :rc-container-id     (rc-state/mk-container-id ctx rc-type)}]

          (rc-state/validate-ctx rc-type ctx)
          (-> (into [mk-container (merge passed-in-data rc-data (:xs-data opts))] (subvec form (if passed-in-data 3 2)))
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

        (swap! stack-atom assoc rc-component-id {:config-stack config-stack :parent-handle handle})
        (-> (into [mk-component (merge passed-in-data rc-data (:xs-data opts))]
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



(defn- resolve-container-instance [parent-handle {:keys [rc-type rc-ctx]}]
  (let [container-name rc-type
        state-atom (get @rc-state/recontain-settings-atom :state-atom)
        cfg (get @rc-state/container-configurations container-name)


        ;create path from parent-path, ctx and container-name. Use diff in ctx
        path (let [context-p (->> (first (data/diff rc-ctx (:ctx parent-handle))) keys sort (map rc-ctx) (reduce str))
                   p (-> (into [] (drop-last (:local-state-path parent-handle)))
                         (into [container-name context-p :_local-state]))]
               (->> p
                    (remove nil?)
                    (remove string/blank?)
                    vec))



        foreign-state-paths (if-let [f (:foreign-state cfg)]
                              (f rc-ctx)
                              {})

        foreign-local-state-ids (reduce-kv (fn [m k v] (when (keyword? v) (assoc m k (rc-state/mk-container-id rc-ctx v))))
                                           {}
                                           foreign-state-paths)

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/handles-atom [v :local-state-path])))
                                             {}
                                             foreign-local-state-ids)

        all-state-paths (-> foreign-state-paths
                            (merge foreign-local-state-paths)
                            (assoc container-name path))]



    (merge cfg {:parent-handle           parent-handle
                :reload-conf-count       @rc-state/reload-configuration-count
                :path                    path
                :all-paths               all-state-paths

                :reactions               (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom v nil)))) {} all-state-paths)
                :foreign-local-state-ids foreign-local-state-ids})))


(defn mk-container [{:keys [rc-type rc-ctx rc-parent-handle-id rc-container-id] :as data-in}]
  (let [container-name rc-type
        parent-handle (get @rc-state/handles-atom rc-parent-handle-id)
        cfg (reaction (resolve-container-instance parent-handle data-in))
        ;org-handle (rc-state/mk-container-handle parent-handle ctx @cfg)

        ]


    (r/create-class
      {:component-did-mount    (fn [_]
                                 (let [{:keys [container-name did-mount]} @cfg]
                                   ;;todo wrap in try catch
                                   (rc-state/debug #(println "DID MOUNT - " container-name))
                                   #_(when did-mount (did-mount org-handle))))

       :component-will-unmount (fn [_]
                                 (let [{:keys [container-name will-mount]} @cfg]
                                   (rc-state/debug #(println "WILL UNMOUNT - " container-name))
                                   #_(when will-mount (will-mount org-handle))
                                   #_(when (:clear-container-state-on-unmount? @rc-state/recontain-settings-atom)
                                       (rc-state/clear-container-state org-handle))))

       :reagent-render         (fn [data]
                                 (let [
                                       cfg-config @cfg
                                       start (.getTime (js/Date.))
                                       reload-conf-count (:reload-conf-count @cfg)
                                       _ (rc-state/debug #(println "RENDER - " reload-conf-count container-name))

                                       foreign-local-state-ids (:foreign-local-state-ids cfg-config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions cfg-config))

                                       ;handle foreign states first - because they are passed to local-state-fn

                                       ;; foreign local states are found in component cache - because they have been initialized by renderings
                                       ;; higher up the tree
                                       foreign-local-states (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/handles-atom [v :local-state])))
                                                                       {}
                                                                       foreign-local-state-ids)

                                       foreign-states (-> state-map
                                                          (dissoc container-name)
                                                          (merge foreign-local-states))

                                       local-state (if-let [ls (get state-map container-name)]
                                                     ls
                                                     (if-let [ls-fn (:local-state (get @rc-state/container-configurations container-name))]
                                                       (ls-fn foreign-states)
                                                       {}))

                                       raw-config (rc-state/get-container-config container-name)

                                       handle {:handle-id        rc-container-id
                                               :handle-type      :container
                                               :parent-handle-id rc-parent-handle-id

                                               :config-name      container-name
                                               :ctx              rc-ctx

                                               :local-state      local-state
                                               :local-state-path (:path cfg-config)

                                               :foreign-states   foreign-states
                                               :foreign-paths    (-> cfg-config
                                                                     :all-paths
                                                                     (dissoc container-name)
                                                                     vals
                                                                     vec)}

                                       container-config (->> raw-config
                                                             keys
                                                             (remove keyword?)
                                                             vec
                                                             (select-keys raw-config))

                                       new-config-stack (rc-config-stack/mk handle container-name container-config)

                                       _ (swap! rc-state/handles-atom assoc rc-container-id (assoc handle :raw-config-stack new-config-stack))

                                       rendered (options-value new-config-stack data :render)]

                                   (if-not rendered
                                     [:div (str "No render: " container-name "(Container)")]


                                     ;instead of reagent calling render function - we do it
                                     (let [start (.getTime (js/Date.))
                                           result (-> rendered
                                                      (decorate-hiccup {:component-data {}
                                                                        :handle         handle
                                                                        :config-stack   new-config-stack}))


                                           ;_ (println "Decorate time" container-name (- (js/Date.) start))



                                           ;_ (println "render-container: " container-name (- (.getTime (js/Date.)) start))
                                           ]

                                       result))))})))


(defn- lp [p rc-name rc-key]
  (if rc-key
    (conj p rc-name rc-key)
    (conj p rc-name)))

(defn mk-component [data]
  (let [{:keys [rc-name rc-type rc-component-id rc-key rc-ctx]} data
        {:keys [config-stack parent-handle]} (get @stack-atom rc-component-id)
        state-atom* (get @rc-state/recontain-settings-atom :state-atom)

        config (rc-state/get-config rc-type)
        path (-> (:local-state-path parent-handle) drop-last vec (#(if rc-key
                                                                     (conj % rc-name rc-key :_local-state)
                                                                     (conj % rc-name :_local-state))))

        local-state* (reaction (get-in @state/state path))

        foreign-state-paths (if-let [f (:foreign-state config)]
                              (f data)
                              {})


        foreign-state-reactions* (reduce-kv (fn [m k v]
                                              (assoc m k (reaction (get-in @state-atom* v)))) {} foreign-state-paths)

        ;_ (rc-config-stack/print-config-stack config-stack)

        config-stack (rc-config-stack/shave-by-component config-stack rc-name)
        raw-configs (reaction (rc-state/get-config-with-inherits rc-type))
        ]

    (r/create-class
      {:component-will-unmount (fn [_] #_(swap! stack-atom dissoc rc-component-id))

       :reagent-render         (fn [input-data]
                                 (let [foreign-states (reduce-kv (fn[m k v] (assoc m k (deref v))) {} foreign-state-reactions*)

                                       ;initialize local state with foreign states
                                       local-state (or @local-state* (if-let [f (:local-state config)]
                                                                       (f (merge input-data foreign-states)))
                                                                       {})
                                       ;do overriding validations

                                       ;local-state

                                       handle {:handle-id        rc-component-id
                                               :handle-type      :component
                                               :parent-handle-id (:handle-id parent-handle)

                                               :config-name      rc-type
                                               :ctx              rc-ctx

                                               :local-state      local-state
                                               :local-state-path path

                                               :foreign-states   foreign-states}


                                       ;  _ (println "raw config" raw-configs)
                                       new-config-stack (->> @raw-configs
                                                             (reduce
                                                               (fn [stack [c-name config]] (rc-config-stack/add-config stack handle c-name config))
                                                               config-stack))
                                       _ (swap! rc-state/handles-atom assoc rc-component-id (assoc handle :raw-config-stack new-config-stack))

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