(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [reagent.core :as r]
            [recontain.impl.state :as rc-state]
            [recontain.impl.element :as rc-element]))


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
  (let [style-fn (get (rc-state/get-container-config (:container-name h)) [id :style])
        passed-keys (->> opts keys (filter namespace))
        passed-values (select-keys opts passed-keys)
        ls-with-passed-values (merge ls passed-values)

        style-config (when style-fn
                       (binding [rc-state/*current-container* {:handle         h
                                                               :local-state    ls-with-passed-values
                                                               :foreign-states fs}]
                         (style-fn h)))]
    (-> (apply dissoc opts passed-keys)
        (assoc :id (-> h :container-id (rc-state/dom-element-id id)))
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
      (throw (js/Error. (str "Rendering " (:container-name h) " contains invalid recontain/container structure: First element should be a map of additional context - exampled by this {:team-id 23}). Hiccup: " form))))
    (when-not container-name
      (throw (js/Error. (str "Render function of " (:container-name h) " contains invalid recontain/container structure: Second element should be a keyword referencing the container in config. Hiccup: " form))))
    (when-not (get @rc-state/container-configurations container-name)
      (throw (js/Error. (str "No container configuration found for " container-name ". Please add to configuration."))))

    (let [ctx (merge additional-ctx (:ctx h))]
      (rc-state/validate-ctx container-name ctx)
      (-> (into [container-fn (assoc ctx :rc_parent-container-id (:container-id h)) container-name] (subvec form 3))
          (with-meta (meta form))))))

(defn get-element-config [h form]
  (let [local-name (keyword (name (first form)))]
    (reduce-kv (fn [m k v]
                 (if (and (vector? k) (= local-name (first k)))
                   (assoc m (subvec k 1) v)
                   m))
               {}
               (rc-state/get-container-config (:container-name h)))
    ))

(defn decorate-hiccup-result [form h ls fs]
  (cond
    (rc-element/element-form? form)
    (rc-element/decorate-element form {:parent-dom-id      (:container-id h)
                                       :parent-path    (:path h)
                                       :parent-configs [(get-element-config h form)]})

    (and (vector? form) (= (first form) @rc-state/container-fn))
    (decorate-container form h)

    ;;vector of special form with ::keyword
    (and (vector? form) (keyword? (first form)) (namespace (first form)))
    (let [local-id (keyword (name (first form)))
          opts (-> (if (map? (second form)) (second form) {}) (assoc :id local-id))
          elm (if (:elm opts) (:elm opts) :div)

          ;decorate children
          children (->> (if (map? (second form)) (nnext form) (next form))
                        (map (fn [c] (decorate-hiccup-result c h ls fs)))
                        vec)]

      (-> [elm (bind-options h ls fs (dissoc opts :elm))]
          (into children)
          ;preserve meta
          (with-meta (meta form))))

    (vector? form)
    (let [v (->> form (map (fn [c] (decorate-hiccup-result c h ls fs))) vec)]
      ;preserve meta
      (with-meta v (meta form)))

    (sequential? form)
    (->> form (map (fn [c] (decorate-hiccup-result c h ls fs))))

    :else form))


(defn- resolve-container-instance [parent-handle ctx container-name]
  (let [state-atom (get @rc-state/recontain-settings-atom :state-atom)
        cfg (get @rc-state/container-configurations container-name)

        ;create path from parent-path, ctx and container-name. Use diff in ctx
        path (let [context-p (->> (first (data/diff ctx (:ctx parent-handle))) keys sort (map ctx) (reduce str))
                   p (-> (into [] (drop-last (:path parent-handle)))
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

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @rc-state/container-states-atom [v :path])))
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
        org-handle (rc-state/mk-handle parent-handle ctx @cfg)]

    (r/create-class
      {:component-did-mount    (fn [_]
                                 (let [{:keys [container-name did-mount]} @cfg]
                                   ;;todo wrap in try catch
                                   (rc-state/debug #(println "DID MOUNT - " container-name))
                                   (when did-mount (did-mount org-handle))))

       :component-will-unmount (fn [_]
                                 (let [{:keys [container-name will-mount]} @cfg]
                                   (rc-state/debug #(println "WILL UNMOUNT - " container-name))
                                   (when will-mount (will-mount org-handle))
                                   (when (:clear-container-state-on-unmount? @rc-state/recontain-settings-atom)
                                     (rc-state/clear-container-state org-handle))))

       :reagent-render         (fn [_ _ opts]
                                 (let [{:keys [container-name] :as config} @cfg
                                       _ (rc-state/debug #(println "RENDER - " container-name))

                                       container-id (:container-id config)
                                       foreign-local-state-ids (:foreign-local-state-ids config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions config))
                                       handle (rc-state/mk-handle parent-handle ctx config)

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

                                       _ (swap! rc-state/container-states-atom assoc container-id {:handle         handle
                                                                                                   :local-state    local-state
                                                                                                   :foreign-states foreign-states})
                                       render (:render config)]

                                   (if-not render
                                     [:div (str "No render: " config)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result (binding [rc-state/*current-container* {:handle         handle
                                                                                          :local-state    local-state
                                                                                          :foreign-states foreign-states}]
                                                    (-> (render handle opts)
                                                        (decorate-hiccup-result handle local-state foreign-states)))]

                                       (if-let [decorator (:component-hiccup-decorator @rc-state/recontain-settings-atom)]
                                         (decorator result (get @rc-state/container-states-atom container-id))
                                         result)))))})))

