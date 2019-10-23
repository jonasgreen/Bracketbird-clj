(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [reagent.core :as r]
            [recontain.impl.state :as rc-state]
            [bracketbird.state :as state]))


(defn event-key? [k]
  (string/starts-with? (if (sequential? k) (name (last k)) (name k)) "on-"))


(defn bind-config-value [h k v]
  (if (fn? v)
    (fn [volatile-state]
      (binding [rc-state/*current-handle* (update h :volatile-state merge volatile-state)]
        (v rc-state/*current-handle*)))
    v))



(defn name-in-local-state [sub-id value-name]
  (keyword (if-not (string/blank? sub-id)
             (str (name sub-id) "-" (name value-name))
             (name value-name))))


(defn dispatch-silent [h f & args]
  (rc-state/dispatch {:handle h :dispatch-f f :args args :silently-fail? true}))


(def event-bindings {:hover  {:on-mouse-enter (fn [h sub-id _ _] (rc-state/put! h assoc (name-in-local-state sub-id "hover?") true))
                              :on-mouse-leave (fn [h sub-id _ _] (rc-state/put! h assoc
                                                                                (name-in-local-state sub-id "hover?") false
                                                                                (name-in-local-state sub-id "active?") false))}

                     :focus  {:on-focus (fn [h sub-id _ _] (rc-state/put! h assoc (name-in-local-state sub-id "focus?") true))
                              :on-blur  (fn [h sub-id _ _] (rc-state/put! h assoc (name-in-local-state sub-id "focus?") false))}

                     :change {:on-change (fn [h sub-id _ e] (rc-state/put! h assoc (name-in-local-state sub-id "value") (.. e -target -value)))}

                     :click  {:on-click      (fn [_ _ _ _] ())
                              :on-mouse-down (fn [h sub-id _ _] (rc-state/put! h assoc (name-in-local-state sub-id "active?") true))
                              :on-mouse-up   (fn [h sub-id _ _] (rc-state/put! h assoc (name-in-local-state sub-id "active?") false))}

                     :key    {:on-key-down (fn [h sub-id ls e]
                                             (let [backspace? (= 8 (.-keyCode e))
                                                   delete? (get ls (name-in-local-state sub-id "delete-on-backspace?"))]
                                               (when (and backspace? delete?)
                                                 (.stopPropagation e)
                                                 (dispatch-silent h [sub-id :delete-on-backspace]))))
                              :on-key-up   (fn [h sub-id _ e]
                                             (when (= "text" (.-type (.-target e)))
                                               (rc-state/put! h assoc (name-in-local-state sub-id "delete-on-backspace?") (clojure.string/blank? (.. e -target -value)))))}

                     :scroll {:on-scroll (fn [h sub-id _ e] (let [t (.-target e)
                                                                  scroll-top (.-scrollTop t)
                                                                  scroll-height (.-scrollHeight t)
                                                                  client-height (.-clientHeight t)]

                                                              (rc-state/put! h assoc
                                                                             (name-in-local-state sub-id "scroll-top") scroll-top
                                                                             (name-in-local-state sub-id "scroll-height") scroll-height
                                                                             (name-in-local-state sub-id "client-height") client-height
                                                                             (name-in-local-state sub-id "scroll-bottom") (- scroll-height scroll-top client-height))))}})
(defn- bind-event-map [opts e-map h sub-id ls passed-values]
  (reduce-kv (fn [m event-name event-action]
               (assoc m event-name (fn [e]
                                     (event-action h sub-id ls e)
                                     (binding [rc-state/*passed-values* passed-values]
                                       (dispatch-silent h [sub-id event-name] e)))))
             opts
             e-map))

(defn- bind-events
  [{:keys [events] :as opts} sub-id h ls passed-values]
  (if events
    (->> (if (sequential? events) events [events])
         (reduce (fn [m bind-name]
                   (if-let [e-map (get event-bindings bind-name)]
                     (bind-event-map m e-map h sub-id ls passed-values)
                     (println "No event binding found for " bind-name)))
                 opts))
    opts))

(defn bind-options [h ls fs {:keys [id] :as opts}]
  (let [style-fn (get (rc-state/get-container-config (:config-name h)) [id :style])
        passed-keys (->> opts keys (filter namespace))
        passed-values (select-keys opts passed-keys)
        ls-with-passed-values (merge ls passed-values)

        style-config (when style-fn
                       (binding [rc-state/*current-container* {:handle         h
                                                               :local-state    ls-with-passed-values
                                                               :foreign-states fs}]
                         (style-fn h)))]
    (-> (apply dissoc opts passed-keys)
        (assoc :id (-> h :container-id (rc-state/dom-id id)))
        (bind-events id h ls passed-values)
        (assoc :style (or style-config (:style opts) {})))))

(defn external-bind-options [opts]
  (let [{:keys [handle local-state foreign-state]} rc-state/*current-container*]
    (bind-options handle local-state foreign-state (if (keyword? opts) {:id opts} opts))))

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
      (-> (into [container-fn (assoc ctx :rc_parent-container-id (:handle-id h)) container-name] (subvec form 3))
          (with-meta (meta form))))))


(defn- filter-config [element-ref config]
  (reduce-kv (fn [m k v]
               (if (and (vector? k) (= element-ref (first k)))
                 (assoc m (subvec k 1) v)
                 m))
             {}
             config))

(defn filter-configs [element-ref configs]
  (->> configs
       (map #(filter-config element-ref %))
       vec))

(defn- parent-function-keyword [k]
  (keyword (str (name k) "'")))

;dont call directly - use merge-configs
(defn- merge-configs-maps [newest oldest]
  (->> newest
       (reduce-kv (fn [m k v]
                    (if-let [existing-value (get m k)]
                      (assoc m [(first k) (parent-function-keyword (last k))] existing-value
                               k v)
                      (assoc m k v)))
                  oldest)))

(defn merge-configs [config-xs]
  (loop [xs config-xs]
    (if (< (count xs) 2)
      (first xs)
      (recur (let [merged (merge-configs-maps (-> xs drop-last last) (last xs))]
               (-> (drop-last 2 xs) vec (conj merged)))))))



(defn- append-missing-element-refs [element-ref first-element? config]
  (if first-element?
    (reduce-kv (fn [m k v] (if (and (vector? k) first-element? (= 1 (count k)))
                             (assoc m [element-ref (first k)] v)
                             m))
               config
               config)
    config))

(defn- remove-none-element-keys [element-ref config]
  (reduce-kv (fn [m k v] (if (and (vector? k) (= (first k) element-ref))
                           (assoc m k v)
                           m))
             {}
             config))

(defn- convert-vector-to-option-keys [config]
  (reduce-kv (fn [m k v] (assoc m (last k) v))
             {}
             config))


(defn- mk-element
  "Returns a vector containing the element and the elements options"
  [element-opts handle configs]
  (let [{:keys [first-element? element-ref]} element-opts
        {:keys [handle-id local-state]} handle

        elm (or (:elm element-opts) :div)
        passed-keys (->> element-opts keys (filter namespace))
        passed-values (select-keys element-opts passed-keys)

        _ (println element-ref "--- passed values" passed-values)
        ;keyword -> value config
        element-config (->> configs
                            (map (partial append-missing-element-refs element-ref first-element?))
                            (map (partial remove-none-element-keys element-ref))
                            (merge-configs)

                            ;[:xxx :style] -> :style
                            (convert-vector-to-option-keys))

        _ (println element-ref "element-config keys" (keys element-config))
        _ (println element-ref "option keys" (keys element-opts))


        ;assemble options
        options (reduce-kv (fn [m k v]
                             (let [parent-fn-key (parent-function-keyword k)
                                   parent-fn (get element-config parent-fn-key)
                                   volatile-state (merge passed-values (when parent-fn {parent-fn-key parent-fn}))]

                               (if (event-key? k)
                                 (assoc m k (fn [e] (v (merge volatile-state {:event e})))) ;wrap events for later execution
                                 (assoc m k (v volatile-state))))) {} element-config)
        _ (println element-ref "final option keys" (keys options))

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


(defn decorate-component [form {:keys [component-dom-id component-path component-configs]}]
  (-> (into [@rc-state/component-fn {:component-id   (keyword (name (second form)))
                                     :parent-path    component-path
                                     :parent-dom-id  component-dom-id
                                     :parent-configs component-configs}] (subvec form 2))
      (with-meta (meta form))))



(defn decorate-hiccup [form first-element? handle configs]
  (cond
    ;;container
    (and (vector? form) (= (first form) @rc-state/container-fn))
    (decorate-container form handle)

    ;;component
    (component-form? form)
    (decorate-component form {:parent-dom-id  (:handle-id handle)
                              :parent-path    (:local-state-path handle)
                              :parent-configs (filter-configs (keyword (name (first form))) configs)})


    ;;vector of special form with ::keyword
    (and (vector? form) (keyword? (first form)) (namespace (first form)))
    (let [;;TODO- if element render is present in config - then recur with that content

          children (->> (if (map? (second form)) (nnext form) (next form))
                        (map (fn [f] (decorate-hiccup f false? handle configs)))
                        vec)
          element-opts (-> (if (map? (second form)) (second form) {})
                           (assoc :element-ref (keyword (name (first form)))
                                  :first-element? first-element?))]

      (-> (mk-element element-opts handle configs)
          (into children)
          ;preserve meta
          (with-meta (meta form))))

    (vector? form)
    (let [v (->> form (map (fn [f] (decorate-hiccup f first-element? handle configs))) vec)]
      ;preserve meta
      (with-meta v (meta form)))

    (sequential? form)
    (->> form (map (fn [f] (decorate-hiccup f first-element? handle configs))))

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
        container-id (rc-state/mk-container-id ctx container-name)

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
                :container-id            container-id
                :reload-conf-count       @rc-state/reload-configuration-count
                :path                    path
                :all-paths               all-state-paths
                :reactions               (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom v nil)))) {} all-state-paths)
                :foreign-local-state-ids foreign-local-state-ids})))

(defn- mk-container [additional-ctx container-name _]
  (let [parent-handle-id (:rc_parent-container-id additional-ctx)
        parent-handle (:handle (get @rc-state/container-states-atom parent-handle-id))

        ;:rc_parent-container-id is set during decoration when rendering previous container
        ctx (-> (:ctx parent-handle) (merge additional-ctx) (dissoc :rc_parent-container-id))

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

                                       _ (rc-state/debug #(println "RENDER - " container-name))

                                       handle-id (:container-id cfg-config)
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
                                               :volatile-state   {}

                                               :foreign-states   foreign-states
                                               :foreign-paths    (-> cfg-config
                                                                     :all-paths
                                                                     (dissoc container-name)
                                                                     vals
                                                                     vec)}

                                       config (->> raw-config
                                                   (reduce-kv (fn [m k v]
                                                                (let [f (bind-config-value handle k v)]
                                                                  (assoc m k f))) {}))


                                       _ (swap! rc-state/container-states-atom assoc handle-id handle)
                                       render (:render config)]



                                   (if-not render
                                     [:div (str "No render: " config)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result (-> (render handle opts)
                                                      (decorate-hiccup true handle [config]))]

                                       (if-let [decorator (:component-hiccup-decorator @rc-state/recontain-settings-atom)]
                                         (decorator result (get @rc-state/container-states-atom handle-id))
                                         result)))))})))


(defn mk-component
  [{:keys [component-id parent-dom-id parent-path parent-configs]} org-value]
  (let [path (-> parent-configs drop-last vec (conj :_local-state))
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
                                               :volatile-state   {}

                                               :foreign-states   nil
                                               :foreign-paths    nil}

                                       component-config (->> raw-config
                                                             (reduce-kv (fn [m k v] (assoc m k (bind-config-value handle k v))) {}))


                                       render-fn (:render component-config)]


                                   (if-not render-fn
                                     [:div (str "No render: " component-id)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result
                                           (-> (render-fn local-state)
                                               (decorate-hiccup true handle (conj parent-configs component-config)))]
                                       result
                                       ))))})))