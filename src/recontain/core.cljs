(ns recontain.core
  (:refer-clojure :exclude [update])
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.dom :as dom]
            [reagent.core :as r]
            [recontain.setup :as s]
            [clojure.string :as string]
            [bracketbird.dom :as d]
            [bracketbird.util :as ut]
            [restyle.core :as rs]))


(defonce component-states-atom (atom {}))
(defonce config-atom (atom {}))
(defonce rerender-atom (r/atom 0))

(def ^:dynamic *current-container* nil)
(def ^:dynamic *passed-values* nil)



(declare container bind-events element-id)


(defn setup [config]
  (reset! component-states-atom {})
  (reset! config-atom (s/resolve-config config))
  @config-atom)

(defn- get-state-atom [] (:state-atom @config-atom))

(defn- debug [f]
  (when (:debug? @config-atom) (f)))

(defn- get-hook-value [hook]
  (let [hook-value (get-in @config-atom [:hooks hook])]
    (when-not hook-value
      (do
        (throw (js/Error. (str "Unable to find mapping for hook: " hook " in config")))))

    hook-value))

(defn- ui-hook? [hook]
  (s/ui-hook? (get-hook-value hook)))

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

(defn- validate-layout [parent-handle hook])

(defn- resolve-path-from-ctx [hook given-ctx]
  (let [{:keys [path ctx]} (get-hook-value hook)]
    (reduce (fn [v p]
              (if (set? p)
                (if-let [ctx-value (get given-ctx (first p))]
                  (conj v ctx-value)
                  (throw (js/Error. (str "Missing context " p " for hook " hook ". Given ctx: " given-ctx ". Required ctx: " ctx))))
                (conj v p)))
            []
            path)))

(defn hook-path [hook ctx]
  (validate-ctx hook ctx)
  (let [p (resolve-path-from-ctx hook ctx)]
    (if (ui-hook? hook)
      (conj p :_local-state)
      p)))


(defn- decorate-container [form h ls fs]
  (let [container-fn (first form)
        additional-ctx (second form)
        hook (nth form 2)]

    (let [hook-key (if (fn? hook)
                     (get (:render-to-hook @config-atom) hook)
                     hook)]

      (when-not (map? additional-ctx)
        (throw (js/Error. (str "Rendering " (:hook h) " contains invalid recontain/container structure: First element should be a map of additional context - exampled by this {:team-id 23}). Hiccup: " form))))
      (when-not hook
        (throw (js/Error. (str "Render function of " (:hook h) " contains invalid recontain/container structure: Second element should be either a keyword referencing the container in config or the render function of the container. Hiccup: " form))))

      (let [ctx (merge additional-ctx (:ctx h))]
        (try
          (validate-ctx hook-key ctx)
          (validate-layout h hook-key)
          (catch :default e (throw (js/Error (str "Error while rendering " (:hook h) ". " e)))))


        (-> (into [container-fn (assoc ctx :rc_handle h) hook-key] (subvec form 3))
            (with-meta (meta form)))))))

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
    (decorate-container form h ls fs)

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



(defn force-render-all []
  (swap! rerender-atom inc))

(defn- resolve-container-config [ctx hook]
  (let [state-atom (get @config-atom :state-atom)
        config (get-in @config-atom [:hooks hook])

        all-hooks (into [hook] (:subscribe config))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        foreign-local-state-ids (->> (dissoc all-paths hook)
                                     keys
                                     (filter ui-hook?)
                                     (select-keys all-paths)
                                     (reduce-kv (fn [m k v] (assoc m k (hash v))) {}))

        id (hash (get all-paths hook))
        path (get all-paths hook)]

    {:load-config-count       @rerender-atom
     :container-config        config
     :id                      id
     :path                    path
     :all-hooks               all-hooks
     :all-paths               all-paths
     :reactions               (reduce (fn [m h] (assoc m h (reaction (get-in @state-atom (h all-paths) nil)))) {} all-hooks)
     :foreign-local-state-ids foreign-local-state-ids
     }))

(defn- mk-handle [parent-handle ctx hook {:keys [id path all-paths]}]
  {:parent-handle parent-handle
   :id            id
   :ctx           ctx
   :path          path
   :hook          hook
   :foreign-paths (-> all-paths
                      (dissoc hook)
                      vals
                      vec)})

(defn- gui [parent-handle ctx hook _]
  (let [cfg (reaction (resolve-container-config ctx hook))
        org-handle (mk-handle parent-handle ctx hook @cfg)]

    (r/create-class
      {:component-did-mount    (fn [_]
                                 ;;todo wrap in try catch
                                 (debug #(println "DID MOUNT - " hook))
                                 (when-let [f (:did-mount (:container-config @cfg))]
                                   (f org-handle)))

       :component-will-unmount (fn [this]
                                 (debug #(println "WILL UNMOUNT - " hook))
                                 (when-let [f (:will-unmount (:container-config @cfg))]
                                   (f org-handle))
                                 (when (:clear-container-state-on-unmount? @config-atom)
                                   (clear-container-state org-handle)))

       :reagent-render         (fn [_ _ _ opts]
                                 (debug #(println "RENDER - " hook))
                                 (let [config @cfg
                                       lc-count (:load-config-count config)
                                       _ (debug #(println "RERENDER-COUNT" lc-count))

                                       container-config (:container-config config)
                                       id (:id config)
                                       foreign-local-state-ids (:foreign-local-state-ids config)
                                       state-map (reduce-kv (fn [m k v] (assoc m k (deref v))) {} (:reactions config))
                                       handle (mk-handle parent-handle ctx hook config)


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
                                                     (if-let [ls-fn (get-in @config-atom [:hooks hook :local-state])]
                                                       (ls-fn foreign-states)
                                                       {}))

                                       _ (swap! component-states-atom assoc id {:handle         handle
                                                                                :local-state    local-state
                                                                                :foreign-states foreign-states})
                                       render (:render container-config)]

                                   (if-not render
                                     [:div (str "No render: " hook ctx)]

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
  (hash (hook-path hook ctx)))

;;;; API

(defn element-id [handle sub-id] (str (:id handle) "#" (if (keyword? sub-id) (name sub-id) sub-id)))


(defn- get-handle-data
  ([ctx hook]
   (get-handle-data (mk-id ctx hook)))
  ([id]
   (get @component-states-atom id)))


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
        f (-> @config-atom
              (get-in [:hooks hook])
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
  (:handle (get-handle-data ctx hook)))

(defn get-local-state [id]
  (-> id get-handle-data :local-state))

(defn get-data
  ([handle hook]
   (get-data handle {} hook))
  ([handle extra-ctx hook]
   (get-in @(get-state-atom) (hook-path hook (merge (:ctx handle) extra-ctx)))))

(defn ctx
  ([handle] (:ctx handle))
  ([handle extra-ctx] (merge (:ctx handle) extra-ctx)))

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


;; TODO - support direct calls instead of lazy hiccup
(defn container
  ([ctx hook]
   (container ctx hook {}))

  ([ctx hook optional-value]
   ;rc_handle is set during decoration when rendering previous container
   [gui (:rc_handle ctx) (dissoc ctx :rc_handle) hook optional-value]))