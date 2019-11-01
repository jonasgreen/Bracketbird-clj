(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [reagent.core :as r]
            [recontain.impl.state :as rc-state]
            [recontain.impl.config-stack :as rc-config-stack]
            [bracketbird.state :as state]))


(defn event-key? [k]
  (string/starts-with? (if (sequential? k) (name (last k)) (name k)) "on-"))

(defn bind-config-value [handle stack-index element-ref v]
  (if (fn? v)
    (fn [config-stack element-state]
      (binding [rc-state/*current-handle* (-> handle
                                              (update :element-state merge element-state)
                                              (assoc :element-ref element-ref)
                                              (assoc :config-stack-index stack-index
                                                     :config-stack config-stack))]
        (v rc-state/*current-handle*)))
    v))

(defn bind-config-values
  ([handle stack-index config]
   (bind-config-values handle stack-index nil config))

  ;optional element-ref
  ([handle stack-index element-ref config]
   (reduce-kv (fn [m k v] (let [e-ref (or element-ref (when (vector? k) (first k)))]
                            (assoc m k (bind-config-value handle stack-index e-ref v)))) {} config)))


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
  (let [{:keys [first-element? element-ref elm]} element-opts
        {:keys [handle-id local-state]} handle

        ;;ready element configs

        element-opts-config (->> (dissoc element-opts :elm :first-element? :element-ref :inherit)
                                 (bind-config-values handle (count (:configs config-stack))))

        ;add element-config
        config-stack (rc-config-stack/add config-stack (str element-ref "-opts") element-opts-config)


        ;;Add inherit configs to stack
        config-stack (reduce (fn [stack inh] (->> inh
                                                  (get @rc-state/element-configurations)
                                                  (bind-config-values handle (count (:configs stack)) element-ref)
                                                  (rc-config-stack/add stack inh)))
                             config-stack
                             (:inherit element-opts))

        elm (or elm :div)
        passed-element-state (->> element-opts keys (filter namespace) (select-keys element-opts))


        ;assemble options
        options (reduce (fn [m k]
                          (if (symbol? k)
                            m
                            (let [{:keys [value index]} (rc-config-stack/config-value config-stack k)]
                              (if (fn? value)
                                (if (event-key? k)
                                  (assoc m k (fn [e] (value config-stack (merge passed-element-state {:event e})))) ;wrap events for later execution
                                  (assoc m k (value config-stack passed-element-state)))
                                (assoc m k value)))))
                        {} (rc-config-stack/config-keys config-stack))

        ]

    [elm (-> options
             (assoc :id (rc-state/dom-id handle-id element-ref)))]))


(defn- namespaced?
  ([item]
   (and (keyword? item) (namespace item)))

  ([item str-namespace]
   (and (keyword? item)
        (= str-namespace (namespace item)))))

(defn component-form? [form]
  (when (vector? form)
    (let [first-item (first form)
          second-item (second form)]
      ;[::team-name :e/text-input ...]
      (and (namespaced? first-item)
           (namespaced? second-item "e")))))


(defn decorate-component [form {:keys [parent-dom-id parent-path config-stack]}]
  (-> (into [@rc-state/component-fn {:component-id  (keyword (name (second form)))
                                     :parent-path   parent-path
                                     :parent-dom-id parent-dom-id
                                     :config-stack  config-stack}] (subvec form 2))
      (with-meta (meta form))))



(defn decorate-hiccup [form first-element? handle config-stack]
  (cond
    ;;container
    (and (vector? form) (= (first form) @rc-state/container-fn))
    (decorate-container form handle)

    ;;component
    (component-form? form)
    (decorate-component form {:parent-dom-id (:handle-id handle)
                              :parent-path   (:local-state-path handle)
                              :config-stack  (rc-config-stack/shave config-stack (keyword (name (first form))))})


    ;;vector of special form with ::keyword
    (and (vector? form) (keyword? (first form)) (namespace (first form)))
    (let [;;TODO- if element render is present in config - then recur with that content
          element-ref (keyword (name (first form)))

          children (->> (if (map? (second form)) (nnext form) (next form))
                        (map (fn [f] (decorate-hiccup f false? handle config-stack)))
                        vec)
          element-opts (-> (if (map? (second form)) (second form) {})
                           (assoc :element-ref element-ref
                                  :first-element? first-element?))]

      (-> (mk-element element-opts handle (rc-config-stack/prepare-for-element config-stack element-ref))
          (into children)
          ;preserve meta
          (with-meta (meta form))))

    (vector? form)
    (let [v (->> form (map (fn [f] (decorate-hiccup f first-element? handle config-stack))) vec)]
      ;preserve meta
      (with-meta v (meta form)))

    (sequential? form)
    (->> form (map (fn [f] (decorate-hiccup f first-element? handle config-stack))))

    :else form))


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

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/container-states-atom [v :local-state-path])))
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
        parent-handle (get @rc-state/container-states-atom parent-handle-id)

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
                                 (let [cfg-config @cfg
                                       start (.getTime (js/Date.))

                                       _ (rc-state/debug #(println "RENDER - " container-name))

                                       handle-id (:handle-id cfg-config)
                                       foreign-local-state-ids (:foreign-local-state-ids cfg-config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions cfg-config))

                                       ;handle foreign states first - because they are passed to local-state-fn

                                       ;; foreign local states are found in component cache - because they have been initialized by renderings
                                       ;; higher up the tree
                                       foreign-local-states (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/container-states-atom [v :local-state])))
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
                                               :raw-config       raw-config
                                               :ctx              ctx

                                               :local-state      local-state
                                               :local-state-path (:path cfg-config)
                                               :element-state    {}

                                               :foreign-states   foreign-states
                                               :foreign-paths    (-> cfg-config
                                                                     :all-paths
                                                                     (dissoc container-name)
                                                                     vals
                                                                     vec)}

                                       config (->> raw-config
                                                   keys
                                                   (remove keyword?)
                                                   vec
                                                   (select-keys raw-config)
                                                   (bind-config-values handle 0))

                                       _ (swap! rc-state/container-states-atom assoc handle-id handle)
                                       render (get config [:render])]



                                   (if-not render
                                     [:div (str "No render: " container-name)]


                                     ;instead of reagent calling render function - we do it
                                     (let [
                                           result (-> (render handle opts)
                                                      (decorate-hiccup true handle (rc-config-stack/mk
                                                                                     container-name
                                                                                     config)))

                                           ;_ (println "render" (- (.getTime (js/Date.)) start) )
                                           ]

                                       ;(println container-name "render time: " (- (.getTime (js/Date.)) start))

                                       (if-let [decorator (:component-hiccup-decorator @rc-state/recontain-settings-atom)]
                                         (decorator result (get @rc-state/container-states-atom handle-id))
                                         result)))))})))


(defn mk-component
  [{:keys [component-id parent-dom-id parent-path config-stack]} org-value]
  (let [path (-> parent-path drop-last vec (conj :_local-state))
        local-state-atom (reaction (get-in state/state path))
        dom-id (rc-state/dom-id parent-dom-id component-id)

        ;listen for reloading elements specification

        ;listen for state changes

        ;optimizations if changes are from local-state only - then dont rebuild config
        ;TODO local state

        ]

    (r/create-class
      {:component-will-unmount (fn [_]
                                 ;todo remove local-state
                                 )

       :reagent-render         (fn [opts value]
                                 (let [_ (println "component-id" component-id)
                                       local-state @local-state-atom
                                       ;do overriding validations

                                       raw-config (rc-state/get-component-config component-id)

                                       handle {:handle-id        dom-id
                                               :handle-type      :component
                                               :parent-handle-id parent-dom-id

                                               :config-name      component-id
                                               :raw-config       raw-config
                                               :ctx              nil

                                               :local-state      local-state
                                               :local-state-path path
                                               :element-state    {}

                                               :foreign-states   nil
                                               :foreign-paths    nil}

                                       component-config (->> raw-config
                                                             (bind-config-values handle (count (:configs config-stack))))


                                       render-fn (get component-config [:render])]


                                   (if-not render-fn
                                     [:div (str "No render: " component-id)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result
                                           (-> (render-fn local-state)
                                               (decorate-hiccup true handle (rc-config-stack/add config-stack component-id component-config)))]
                                       result
                                       ))))})))