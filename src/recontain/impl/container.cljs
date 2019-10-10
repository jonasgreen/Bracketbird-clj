(ns recontain.impl.container
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [reagent.core :as r]
            [recontain.impl.state :as state]
            [restyle.core :as rs]
            [bracketbird.util :as ut]
            [bracketbird.dom :as d]))


(defn- name-in-local-state [sub-id value-name]
  (keyword (if-not (string/blank? sub-id)
             (str (name sub-id) "-" (name value-name))
             (name value-name))))

(defn- put-value [h sub-id k v]
  (state/put! h assoc (name-in-local-state sub-id k) v))

(def events-shorts->event-handlers {:focus  [:on-focus :on-blur]
                                    :hover  [:on-mouse-enter :on-mouse-leave]
                                    :change [:on-change]
                                    :click  [:on-click]
                                    :key    [:on-key-down :on-key-up]
                                    :scroll [:on-scroll]})


(defn dispatch-silent [h f & args]
  (state/dispatch {:handle h :dispatch-f f :args args :silently-fail? true}))

(def event-handler-fns {:on-focus       (fn [h sub-id _ _]
                                          (put-value h sub-id "focus?" true))

                        :on-blur        (fn [h sub-id _ _]
                                          (put-value h sub-id "focus?" false))

                        :on-mouse-enter (fn [h sub-id _ _]
                                          (put-value h sub-id "hover?" true))

                        :on-mouse-leave (fn [h sub-id _ _]
                                          (put-value h sub-id "hover?" false))

                        :on-key-down    (fn [h sub-id ls e]
                                          (let [dob (name-in-local-state sub-id "delete-on-backspace?")]
                                            (d/handle-key e {[:BACKSPACE]
                                                             (fn [_]
                                                               (when (get ls dob)
                                                                 (dispatch-silent h [sub-id :delete-on-backspace])) [:STOP-PROPAGATION])})))
                        :on-key-up      (fn [h sub-id _ e]
                                          (when (= "text" (.-type (.-target e)))
                                            (put-value h sub-id "delete-on-backspace?" (clojure.string/blank? (ut/value e)))))

                        :on-change      (fn [h sub-id _ e]
                                          (put-value h sub-id "value" (ut/value e)))

                        :on-click       (fn [_ _ _ _] ())


                        :on-scroll      (fn [h sub-id _ e] (let [t (.-target e)
                                                                 scroll-top (.-scrollTop t)
                                                                 scroll-height (.-scrollHeight t)
                                                                 client-height (.-clientHeight t)]

                                                             (state/put! h assoc
                                                                         (name-in-local-state sub-id "scroll-top") scroll-top
                                                                         (name-in-local-state sub-id "scroll-height") scroll-height
                                                                         (name-in-local-state sub-id "client-height") client-height
                                                                         (name-in-local-state sub-id "scroll-bottom") (- scroll-height scroll-top client-height))))})

(defn- bind-events
  [{:keys [events] :as opts} local-id h ls passed-values]
  (if events
    (->> (if (sequential? events) events [events])
         ; resolve event to handlers
         (map (fn [e] (if-let [handlers (e events-shorts->event-handlers)]
                        handlers
                        (e event-handler-fns))))
         flatten
         ; resolve handlers
         (reduce (fn [m k]
                   (when-let [f (get event-handler-fns k)]
                     (assoc m k (fn [e]
                                  (f h local-id ls e)
                                  (binding [state/*passed-values* passed-values]
                                    (dispatch-silent h [local-id k] e))))))
                 opts))
    opts))

(defn bind-options [h ls fs {:keys [id] :as opts}]
  (let [style-fn (get (state/get-container-config (:hook h)) [id :style])
        passed-keys (->> opts keys (filter namespace))
        passed-values (select-keys opts passed-keys)
        ls-with-passed-values (merge ls passed-values)

        style-config (when style-fn
                       (binding [state/*current-container* {:handle         h
                                                            :local-state    ls-with-passed-values
                                                            :foreign-states fs}]
                         (style-fn h)))
        ]
    (-> (apply dissoc opts passed-keys)
        (assoc :id (state/dom-element-id h id))
        (bind-events id h ls passed-values)
        (assoc :style (rs/style (or style-config (:style opts) {}))))))

(defn external-bind-options [opts]
  (let [{:keys [handle local-state foreign-state]} state/*current-container*]
    (bind-options handle local-state foreign-state (if (keyword? opts) {:id opts} opts))))

(defn decorate-container [form h]
  (let [container-fn (first form)
        additional-ctx (second form)
        hook (nth form 2)]

    (when-not (map? additional-ctx)
      (throw (js/Error. (str "Rendering " (:hook h) " contains invalid recontain/container structure: First element should be a map of additional context - exampled by this {:team-id 23}). Hiccup: " form))))
    (when-not hook
      (throw (js/Error. (str "Render function of " (:hook h) " contains invalid recontain/container structure: Second element should be a keyword referencing the container in config. Hiccup: " form))))
    (when-not (get @state/container-configurations hook)
      (throw (js/Error. (str "No container configuration found for " hook ". Please add to configuration."))))

    (let [ctx (merge additional-ctx (:ctx h))]
      (state/validate-ctx hook ctx)
      (-> (into [container-fn (assoc ctx :rc_parent-container-id (:id h)) hook] (subvec form 3))
          (with-meta (meta form))))))

(defn decorate-hiccup-result [form h ls fs]
  (cond
    (and (vector? form) (= (first form) @state/container-fn))
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


(defn- resolve-container-instance [parent-handle ctx hook]
  (let [state-atom (get @state/recontain-settings-atom :state-atom)
        cfg (get @state/container-configurations hook)

        ;create path from parent-path, ctx and hook. Use diff in ctx
        path (let [context-p (->> (first (data/diff ctx (:ctx parent-handle))) keys sort (map ctx) (reduce str))
                   p (-> (into [] (drop-last (:path parent-handle)))
                         (into [hook context-p :_local-state]))]
               (->> p
                    (remove nil?)
                    (remove string/blank?)
                    vec))

        ;id of hook instance
        id (state/mk-container-id ctx hook)

        foreign-state-paths (if-let [f (:foreign-state cfg)]
                              (f ctx)
                              {})

        foreign-local-state-ids (reduce-kv (fn [m k v] (when (keyword? v) (assoc m k (state/mk-container-id ctx v))))
                                           {}
                                           foreign-state-paths)

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @state/container-states-atom [v :path])))
                                             {}
                                             foreign-local-state-ids)

        all-state-paths (-> foreign-state-paths
                            (merge foreign-local-state-paths)
                            (assoc hook path))]



    (merge cfg {:parent-handle           parent-handle
                :id                      id
                :reload-conf-count       @state/reload-configuration-count
                :path                    path
                :all-paths               all-state-paths
                :reactions               (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom v nil)))) {} all-state-paths)
                :foreign-local-state-ids foreign-local-state-ids})))

(defn- mk-container [additional-ctx container-hook _]
  (let [parent-handle-id (:rc_parent-container-id additional-ctx)
        parent-handle (:handle (get @state/container-states-atom parent-handle-id))

        ;:rc_parent-container-id is set during decoration when rendering previous container
        ctx (-> (:ctx parent-handle) (merge additional-ctx) (dissoc :rc_parent-container-id))

        cfg (reaction (resolve-container-instance parent-handle ctx container-hook))
        org-handle (state/mk-handle parent-handle ctx @cfg)]

    (r/create-class
      {:component-did-mount    (fn [_]
                                 (let [{:keys [hook did-mount]} @cfg]
                                   ;;todo wrap in try catch
                                   (state/debug #(println "DID MOUNT - " hook))
                                   (when did-mount (did-mount org-handle))))

       :component-will-unmount (fn [_]
                                 (let [{:keys [hook will-mount]} @cfg]
                                   (state/debug #(println "WILL UNMOUNT - " hook))
                                   (when will-mount (will-mount org-handle))
                                   (when (:clear-container-state-on-unmount? @state/recontain-settings-atom)
                                     (state/clear-container-state org-handle))))

       :reagent-render         (fn [_ _ opts]
                                 (let [{:keys [hook] :as config} @cfg
                                       _ (state/debug #(println "RENDER - " hook))

                                       id (:id config)
                                       foreign-local-state-ids (:foreign-local-state-ids config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions config))
                                       handle (state/mk-handle parent-handle ctx config)

                                       ;handle foreign states first - because they are passed to local-state-fn

                                       ;; foreign local states are found in component cache - because they have been initialized by renderings
                                       ;; higher up the tree
                                       foreign-local-states (reduce-kv (fn [m k v] (assoc m k (get-in @state/container-states-atom [v :local-state])))
                                                                       {}
                                                                       foreign-local-state-ids)

                                       foreign-states (-> state-map
                                                          (dissoc hook)
                                                          (merge foreign-local-states))

                                       local-state (if-let [ls (get state-map hook)]
                                                     ls
                                                     (if-let [ls-fn (:local-state (get @state/container-configurations hook))]
                                                       (ls-fn foreign-states)
                                                       {}))

                                       _ (swap! state/container-states-atom assoc id {:handle         handle
                                                                                      :local-state    local-state
                                                                                      :foreign-states foreign-states})
                                       render (:render config)]

                                   (if-not render
                                     [:div (str "No render: " config)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result (binding [state/*current-container* {:handle         handle
                                                                                       :local-state    local-state
                                                                                       :foreign-states foreign-states}]
                                                    (-> (render handle opts)
                                                        (decorate-hiccup-result handle local-state foreign-states)))]

                                       (if-let [decorator (:component-hiccup-decorator @state/recontain-settings-atom)]
                                         (decorator result (get @state/container-states-atom id))
                                         result)))))})))