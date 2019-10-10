(ns recontain.core
  (:refer-clojure :exclude [update])
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [clojure.data :as data]
            [clojure.string :as string]
            [bracketbird.dom :as d]
            [bracketbird.util :as ut]
            [restyle.core :as rs]))


(defonce component-states-atom (atom {}))
(defonce config-atom (atom {}))
(defonce container-configurations (atom {}))

(def ^:dynamic *current-container* nil)
(def ^:dynamic *passed-values* nil)



(declare container bind-events element-id mk-id)

(defn setup [config]
  (reset! component-states-atom {})
  (reset! config-atom (assoc config :anonymous-count 0))
  (reset! container-configurations (reduce (fn [m v] (assoc m (:hook v) v)) {} (:containers config)))
  @config-atom)

(defn- debug [f]
  (when (:debug? @config-atom) (f)))

(defn- get-hook-value [hook]
  (let [hook-value (get @container-configurations hook)]
    (when-not hook-value
      (do
        (throw (js/Error. (str "Unable to find mapping for hook: " hook " in config")))))

    hook-value))

(defn- dissoc-path [state path]
  (if (= 1 (count path))
    (dissoc state (last path))
    (update-in state (vec (drop-last path)) dissoc (last path))))

(defn- clear-container-state [handle]
  (let [{:keys [id path]} handle]
    (swap! component-states-atom dissoc id)
    (swap! (:state-atom @config-atom) dissoc-path (drop-last path))))


(defn- validate-ctx [hook given-ctx]
  (let [rq-ctx (:ctx (get-hook-value hook))
        given-ctx-set (reduce-kv (fn [s k v] (if v (conj s k))) #{} given-ctx)]

    (when-not (clojure.set/subset? rq-ctx given-ctx-set)
      (throw (js/Error. (str "Missing context for hook " hook ". Given ctx: " given-ctx ". Required ctx: " rq-ctx))))))

(defn- next-anonymous-hook []
  (keyword (->> (swap! config-atom update-in [:anonymous-count] inc)
                :anonymous-count
                (str "rc-"))))

(defn- ensure-container-configuration! [c optional-value]
  (cond
    (keyword? c)
    (do (when-not (get @container-configurations c)
          (throw (js/Error. (str "No container configuration found for " c ". Either add configuration or pass configuration to function instead."))))
        c)

    (map? c)
    (let [hook (or (:hook c) (next-anonymous-hook))]
      (if-not (get @container-configurations hook)
        (if-not (:render c)
          (throw (js/Error. (str "Container configuration does not contain a render function.")))
          (do
            (swap! container-configurations assoc hook (assoc c :hook hook))
            hook))
        hook))

    (fn? c)
    (let [hook (next-anonymous-hook)]
      (swap! container-configurations assoc hook {:hook   hook
                                                  :render (fn [_] (if optional-value (c optional-value) (c)))})
      hook)

    :else (throw (js/Error. (str "Wrong parameters passed to recontain.core/container " c)))))

(defn- decorate-container [form h]
  (let [container-fn (first form)
        additional-ctx (second form)
        hook (ensure-container-configuration! (nth form 2) (when (< 3 (count form)) (nth form 3)))]

    (when-not (map? additional-ctx)
      (throw (js/Error. (str "Rendering " (:hook h) " contains invalid recontain/container structure: First element should be a map of additional context - exampled by this {:team-id 23}). Hiccup: " form))))
    (when-not hook
      (throw (js/Error. (str "Render function of " (:hook h) " contains invalid recontain/container structure: Second element should be either a keyword referencing the container in config or the render function of the container. Hiccup: " form))))

    (let [ctx (merge additional-ctx (:ctx h))]
      (try
        (validate-ctx hook ctx)
        (catch :default e (throw (js/Error (str "Error while rendering " (:hook h) ". " e)))))


      (-> (into [container-fn (assoc ctx :rc_parent-container-id (:id h)) hook] (subvec form 3))
          (with-meta (meta form))))))

(defn do-bind-options [h ls fs {:keys [id] :as opts}]
  (let [style-fn (get (get-hook-value (:hook h)) [id :style])
        passed-keys (->> opts keys (filter namespace))
        passed-values (select-keys opts passed-keys)
        ls-with-passed-values (merge ls passed-values)

        style-config (when style-fn
                       (binding [*current-container* {:handle         h
                                                      :local-state    ls-with-passed-values
                                                      :foreign-states fs}]
                         (style-fn h)))
        ]
    (-> (apply dissoc opts passed-keys)
        (assoc :id (element-id h id))
        (bind-events id h ls passed-values)
        (assoc :style (rs/style (or style-config (:style opts) {}))))))

(defn bind-options [opts]
  (let [{:keys [handle local-state foreign-state]} *current-container*]
    (do-bind-options handle local-state foreign-state (if (keyword? opts) {:id opts} opts))))

(defn- decorate-hiccup-result [form h ls fs]
  (cond
    (and (vector? form) (= (first form) container))
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

      (-> [elm (do-bind-options h ls fs (dissoc opts :elm))]
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
  (let [state-atom (get @config-atom :state-atom)
        cfg (get @container-configurations hook)

        ;create path from parent-path, ctx and hook. Use diff in ctx
        path (let [context-p (->> (first (data/diff ctx (:ctx parent-handle))) keys sort (map ctx) (reduce str))
                   p (-> (into [] (drop-last (:path parent-handle)))
                         (into [hook context-p :_local-state]))]
               (->> p
                    (remove nil?)
                    (remove string/blank?)
                    vec))

        ;id of hook instance
        id (mk-id ctx hook)

        foreign-state-paths (if-let [f (:foreign-state cfg)]
                              (f ctx)
                              {})

        foreign-local-state-ids (reduce-kv (fn [m k v] (when (keyword? v) (assoc m k (mk-id ctx v))))
                                           {}
                                           foreign-state-paths)

        foreign-local-state-paths (reduce-kv (fn [m k v] (assoc m k (get-in @component-states-atom [v :path])))
                                             {}
                                             foreign-local-state-ids)

        all-state-paths (-> foreign-state-paths
                            (merge foreign-local-state-paths)
                            (assoc hook path))]



    (merge cfg {:parent-handle           parent-handle
                :id                      id
                :path                    path
                :all-paths               all-state-paths
                :reactions               (reduce-kv (fn [m k v] (assoc m k (reaction (get-in @state-atom v nil)))) {} all-state-paths)
                :foreign-local-state-ids foreign-local-state-ids})))

(defn- mk-handle [parent-handle ctx {:keys [id path hook all-paths]}]
  {:parent-handle-id (:id parent-handle)
   :id               id
   :ctx              ctx
   :path             path
   :hook             hook
   :foreign-paths    (-> all-paths
                         (dissoc hook)
                         vals
                         vec)})

(defn- gui [parent-handle ctx container-hook _]
  (let [cfg (reaction (resolve-container-instance parent-handle ctx container-hook))
        org-handle (mk-handle parent-handle ctx @cfg)]

    (r/create-class
      {:component-did-mount    (fn [_]
                                 (let [{:keys [hook did-mount]} @cfg]
                                   ;;todo wrap in try catch
                                   (debug #(println "DID MOUNT - " hook))
                                   (when did-mount (did-mount org-handle))))

       :component-will-unmount (fn [_]
                                 (let [{:keys [hook will-mount]} @cfg]
                                   (debug #(println "WILL UNMOUNT - " hook))
                                   (when will-mount (will-mount org-handle))
                                   (when (:clear-container-state-on-unmount? @config-atom)
                                     (clear-container-state org-handle))))

       :reagent-render         (fn [_ _ _ opts]
                                 (let [{:keys [hook] :as config} @cfg
                                       _ (debug #(println "RENDER - " hook))

                                       id (:id config)
                                       foreign-local-state-ids (:foreign-local-state-ids config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions config))
                                       handle (mk-handle parent-handle ctx config)

                                       ;handle foreign states first - because they are passed to local-state-fn

                                       ;; foreign local states are found in component cache - because they have been initialized by renderings
                                       ;; higher up the tree
                                       foreign-local-states (reduce-kv (fn [m k v] (assoc m k (get-in @component-states-atom [v :local-state])))
                                                                       {}
                                                                       foreign-local-state-ids)

                                       foreign-states (-> state-map
                                                          (dissoc hook)
                                                          (merge foreign-local-states))

                                       local-state (if-let [ls (get state-map hook)]
                                                     ls
                                                     (if-let [ls-fn (:local-state (get @container-configurations hook))]
                                                       (ls-fn foreign-states)
                                                       {}))

                                       _ (swap! component-states-atom assoc id {:handle         handle
                                                                                :local-state    local-state
                                                                                :foreign-states foreign-states})
                                       render (:render config)]

                                   (if-not render
                                     [:div (str "No render: " config)]

                                     ;instead of reagent calling render function - we do it
                                     (let [result (binding [*current-container* {:handle         handle
                                                                                 :local-state    local-state
                                                                                 :foreign-states foreign-states}]
                                                    (-> (render handle opts)
                                                        (decorate-hiccup-result handle local-state foreign-states)))]

                                       (if-let [decorator (:component-hiccup-decorator @config-atom)]
                                         (decorator result (get @component-states-atom id))
                                         result)))))})))


(defn mk-id [ctx hook]
  (let [ctx-id (->> (get @container-configurations hook)
                    :ctx
                    (select-keys ctx)
                    hash)]
    (->> (str hook "@" ctx-id))))


;;;; API

(defn element-id [handle sub-id] (str (:id handle) "#" (if (keyword? sub-id) (name sub-id) sub-id)))


(defn- get-handle-data [id] (get @component-states-atom id))


(defn update [state {:keys [id path]} & args]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state (get-handle-data id))) (rest args)))]
    (update-in state path upd)))

(defn put! [handle & args]
  (swap! (:state-atom @config-atom) #(apply update % handle args)))

(defn delete-local-state [handle]
  (let [{:keys [id path]} handle]
    (swap! component-states-atom dissoc-path [id :local-state])
    (swap! (:state-atom @config-atom) dissoc-path path)))



(defn- do-dispatch [{:keys [handle dispatch-f args silently-fail?]}]
  (let [{:keys [hook id]} handle
        f (-> @container-configurations
              (get hook)
              (get dispatch-f))]

    (if f
      (binding [*current-container* (update-in (get-handle-data id) [:local-state] merge *passed-values*)]
        (apply f handle args))
      (when-not silently-fail? (throw (js/Error. (str "Dispatch function " dispatch-f " is not defined in hook " hook)))))))

(defn dispatch [h f & args]
  (do-dispatch {:handle         h
                :dispatch-f     f
                :args           args
                :silently-fail? false}))

(defn dispatch-silent [h f & args]
  (do-dispatch {:handle         h
                :dispatch-f     f
                :args           args
                :silently-fail? true}))

(defn get-element [handle sub-id]
  (-> handle (element-id sub-id) dom/getElement))

(defn get-handle [ctx hook]
  (validate-ctx hook ctx)
  (:handle (get-handle-data (mk-id ctx hook))))

(defn has-changed [value org-value]
  (when value
    (if (and (string? value) (string? org-value))
      (if (and (string/blank? value)
               (string/blank? org-value))
        false
        (not= value org-value))
      (not= value org-value))))

(defn focus
  ([handle hook ctx-id ctx-value]
   (when-let [ctx-value (if (map? ctx-value) (get ctx-value ctx-id) ctx-value)]
     (focus handle hook {ctx-id ctx-value})))

  ([handle hook extra-ctx]
   (-> (:ctx handle)
       (merge extra-ctx)
       (get-handle hook)
       (dispatch :focus)))

  ([handle hook]
   (focus handle hook {})))



; event

(defn- name-in-local-state [sub-id value-name]
  (keyword (if-not (string/blank? sub-id)
             (str (name sub-id) "-" (name value-name))
             (name value-name))))

(defn- put-value [h sub-id k v]
  (put! h assoc (name-in-local-state sub-id k) v))

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

                                                             (put! h assoc
                                                                   (name-in-local-state sub-id "scroll-top") scroll-top
                                                                   (name-in-local-state sub-id "scroll-height") scroll-height
                                                                   (name-in-local-state sub-id "client-height") client-height
                                                                   (name-in-local-state sub-id "scroll-bottom") (- scroll-height scroll-top client-height))))

                        })

(def events-shorts->event-handlers {:focus  [:on-focus :on-blur]
                                    :hover  [:on-mouse-enter :on-mouse-leave]
                                    :change [:on-change]
                                    :click  [:on-click]
                                    :key    [:on-key-down :on-key-up]
                                    :scroll [:on-scroll]})

(defn bind-events
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
                                  (binding [*passed-values* passed-values]
                                    (dispatch-silent h [local-id k] e))))))
                 opts))
    opts))

(defn ls [& ks]
  (if (seq ks)
    (get-in (:local-state *current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:local-state *current-container*)))

(defn fs [& ks]
  (if (seq ks)
    (get-in (:foreign-states *current-container*) (if (vector? (first ks)) (first ks) (vec ks)))
    (:foreign-states *current-container*)))




(defn container
  ([ctx c]
   (container ctx c {}))

  ([ctx c optional-value]
   (let [parent-handle-id (:rc_parent-container-id ctx)
         parent-handle (:handle (get @component-states-atom parent-handle-id))

         ;:rc_parent-container-id is set during decoration when rendering previous container
         new-ctx (-> (:ctx parent-handle) (merge ctx) (dissoc :rc_parent-container-id))]

     [gui parent-handle new-ctx c optional-value])))