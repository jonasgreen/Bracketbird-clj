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
      value))
  )

(defn decorate-container [form h]
  (let [container-fn (first form)
        additional-ctx (second form)
        container-name (nth form 2)]

    (when-not (map? additional-ctx)
      (throw (js/Error. (str "Rendering " (:config-name h) " contains invalid recontain/container structure: First element should be a map of additional context - exampled by this {:team-id 23}). Hiccup: " form))))
    (when-not container-name
      (throw (js/Error. (str "Render function of " (:config-name h) " contains invalid recontain/container structure: Second element should be a keyword referencing the container in config. Hiccup: " form))))
    (when-not (get @rc-state/container-configurations container-name)
      (throw (js/Error. (str "No container configuration found for " container-name ". Please add to configuration."))))

    (let [ctx (merge additional-ctx (:ctx h))]
      (rc-state/validate-ctx container-name ctx)
      (-> (into [container-fn (assoc ctx :rc_parent-handle-id (:handle-id h)) container-name] (subvec form 3))
          (with-meta (meta form))))))


(defn- mk-element
  "Returns a vector containing the element and the elements options"
  [element-opts handle config-stack]
  (let [{:keys [rc-element-name] :as data} (-> element-opts :data)
        {:keys [handle-id]} handle

        ;;ready element configs

        ;add element-config
        config-stack (->> (dissoc element-opts :decorate :element :data)
                          (rc-config-stack/add-config config-stack handle (str rc-element-name "-opts")))



        ;;Add decorate configs to stack
        config-stack (reduce (fn [stack deco-config] (->> deco-config
                                                          rc-state/get-config
                                                          (rc-config-stack/add-config stack handle deco-config)))
                             config-stack
                             (:decorate element-opts))

        ;_ (rc-config-stack/print-config-stack config-stack)


        elm (or (:element element-opts) :div)

        ;assemble options
        options (reduce (fn [m k]
                          (if (or (symbol? k) (= :decorate k) (= :element k))
                            m
                            (assoc m k (options-value config-stack data k))))
                        {} (rc-config-stack/config-keys config-stack))]

    [elm (-> options
             (assoc :id (rc-state/dom-id handle-id rc-element-name)))]))


(defn- namespaced?
  ([item]
   (and (keyword? item) (namespace item)))

  ([item str-namespace]
   (and (keyword? item)
        (= str-namespace (namespace item)))))

(defn- container-form? [form]
  (and (vector? form)
       (= (first form) @rc-state/container-fn)))

(defn- component-form? [form]
  (and (vector? form)
       (namespaced? (first form))
       (namespaced? (second form))))

(defn- element-form? [form]
  (and
    (vector? form)
    (namespaced? (first form))))

(defn decorate-hiccup [form opts]
  (let [{:keys [component-data handle config-stack]} opts]
    (cond
      ;;container
      (container-form? form)
      (decorate-container form handle)

      ;;component
      (component-form? form)
      (let [rc-component-ref (keyword (name (first form)))
            rc-component-type (keyword (name (second form))) ;remove namespace
            rc-component-id (rc-state/dom-id (:handle-id handle) rc-component-ref)
            rc-component-key (:key (meta form))

            passed-in-data (when-some [data (when (< 2 (count form)) (nth form 2))]
                             ;passed in data should be a map if
                             (if (map? data)
                               data
                               ;string is considered a child
                               (when-not (string? data)
                                 (throw (js/Error. (str "Passed in data to component " rc-component-ref " is not a map. It should be."))))))

            data (merge {:rc-component-ref  rc-component-ref
                         :rc-component-type rc-component-type
                         :rc-component-key  rc-component-key
                         :rc-component-id   rc-component-id}
                        passed-in-data)]

        (swap! stack-atom assoc rc-component-id {:config-stack config-stack :parent-handle handle})
        (-> (into [@rc-state/component-fn data]
                  (subvec form (if data 3 2)))
            (with-meta (meta form))))

      ;;vector of special form with ::keyword
      (element-form? form)
      (let [rc-element-name (keyword (name (first form)))
            rc-element-type (if (keyword? (second form)) (second form) :div)

            rest-of-form (if (keyword? (second form))
                           (nnext form)
                           (next form))

            passed-in-data (when-some [data (first rest-of-form)]
                             ;passed in data should be a map if
                             (if (map? data)
                               data
                               ;string is considered a child
                               (when-not (string? data)
                                 (throw (js/Error. (str "Passed in data to element " rc-element-name " is not a map. It should be."))))))



            rc-data {:rc-element-type rc-element-type
                     :rc-element-name rc-element-name
                     :rc-element-key  (:key (meta form))}

            ;index-children (filter (or vector? string?) form)

            children (->> (if passed-in-data (next rest-of-form) rest-of-form)
                          (map (fn [f] (decorate-hiccup f opts)))
                          vec)]

        (-> (merge rc-data passed-in-data component-data)
            (mk-element handle (rc-config-stack/shave-by-element config-stack rc-element-name))
            (into children)
            ;preserve meta
            (with-meta (meta form))))

      (vector? form)
      (let [v (->> form (map (fn [f] (decorate-hiccup f opts))) vec)]
        ;preserve meta
        (with-meta v (meta form)))

      (sequential? form)
      (->> form (map (fn [f] (decorate-hiccup f opts))))

      :else form)))



(defn- resolve-container-instance [parent-handle ctx container-name]
  (let [state-atom (get @rc-state/recontain-settings-atom :state-atom)
        cfg (get @rc-state/container-configurations container-name)


        ;create path from parent-path, ctx and container-name. Use diff in ctx
        path (let [context-p (->> (first (data/diff ctx (:ctx parent-handle))) keys sort (map ctx) (reduce str))
                   p (-> (into [] (drop-last (:local-state-path parent-handle)))
                         (into [container-name context-p :_local-state]))]
               (->> p
                    (remove nil?)
                    (remove string/blank?)
                    vec))


        ;continer-id of container instance
        handle-id (rc-state/mk-container-id ctx container-name)

        foreign-state-paths (if-let [f (:foreign-state cfg)]
                              (f ctx)
                              {})

        foreign-local-state-ids (reduce-kv (fn [m k v] (when (keyword? v) (assoc m k (rc-state/mk-container-id ctx v))))
                                           {}
                                           foreign-state-paths)

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/handles-atom [v :local-state-path])))
                                             {}
                                             foreign-local-state-ids)

        all-state-paths (-> foreign-state-paths
                            (merge foreign-local-state-paths)
                            (assoc container-name path))]



    (merge cfg {:parent-handle           parent-handle
                :handle-id               handle-id
                :reload-conf-count       @rc-state/reload-configuration-count
                :path                    path
                :all-paths               all-state-paths
                :reactions               (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom v nil)))) {} all-state-paths)
                :foreign-local-state-ids foreign-local-state-ids})))

(defn- mk-container [additional-ctx container-name _]
  (let [parent-handle-id (:rc_parent-handle-id additional-ctx)
        parent-handle (get @rc-state/handles-atom parent-handle-id)

        ;:rc_parent-container-id is set during decoration when rendering previous container
        ctx (-> (:ctx parent-handle) (merge additional-ctx) (dissoc :rc_parent-handle-id))

        cfg (reaction (resolve-container-instance parent-handle ctx container-name))
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

       :reagent-render         (fn [_ _ opts]
                                 (let [
                                       cfg-config @cfg
                                       start (.getTime (js/Date.))

                                       _ (rc-state/debug #(println "RENDER - " container-name))

                                       handle-id (:handle-id cfg-config)
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

                                       handle {:handle-id        handle-id
                                               :handle-type      :container
                                               :parent-handle-id (:handle-id parent-handle)

                                               :config-name      container-name
                                               :ctx              ctx

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

                                       _ (swap! rc-state/handles-atom assoc handle-id (assoc handle :raw-config-stack new-config-stack))


                                       rendered (options-value new-config-stack {} :render)]

                                   (if-not rendered
                                     [:div (str "No render: " container-name "(Container)")]


                                     ;instead of reagent calling render function - we do it
                                     (let [result (-> rendered
                                                      (decorate-hiccup {:component-data {}
                                                                        :handle         handle
                                                                        :config-stack   new-config-stack}))]

                                       result))))})))



(defn mk-component [data]
  (let [{:keys [rc-component-ref rc-component-type rc-component-id]} data
        {:keys [config-stack parent-handle]} (get @stack-atom rc-component-id)

        path (-> (:local-state-path parent-handle) drop-last vec (conj rc-component-ref) (conj :_local-state))

        local-state-atom (reaction (get-in @state/state path))

        ;_ (rc-config-stack/print-config-stack config-stack)

        config-stack (rc-config-stack/shave-by-component config-stack rc-component-ref)

        raw-configs (reaction (rc-state/config-with-inherits rc-component-type))
        ]

    (r/create-class
      {:component-will-unmount (fn [_] #_(swap! stack-atom dissoc rc-component-id))

       :reagent-render         (fn [_ _ _]
                                 (let [local-state @local-state-atom
                                       ;do overriding validations

                                       handle {:handle-id        rc-component-id
                                               :handle-type      :component
                                               :parent-handle-id (:handle-id parent-handle)

                                               :raw-config-stack nil
                                               :config-name      rc-component-type
                                               :ctx              nil

                                               :local-state      local-state
                                               :local-state-path path}

                                       new-config-stack (->> @raw-configs
                                                             (reduce
                                                               (fn [stack config] (rc-config-stack/add-config stack handle rc-component-type config))
                                                               config-stack))

                                       _ (swap! rc-state/handles-atom assoc rc-component-id (assoc handle :raw-config-stack new-config-stack))

                                       rendered (options-value new-config-stack data :render)]


                                   (if-not rendered
                                     [:div (str "No render: " rc-component-type "(" rc-component-ref ")")]

                                     ;instead of reagent calling render function - we do it
                                     (let [result
                                           (-> rendered
                                               (decorate-hiccup {:component-data data
                                                                 :handle         handle
                                                                 :config-stack   new-config-stack}))]
                                       result
                                       ))))})))