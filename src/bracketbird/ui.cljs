(ns bracketbird.ui
  (:refer-clojure :exclude [update])
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bedrock.util :as b-ut]
            [goog.dom :as dom]
            [reagent.core :as r]))

(declare gui hook-path ui-update mk-hook-handle local-state get-handle foreign-handle dynamic-api)

(defonce component-states (atom {}))

(defn hook? [h]
  (and (keyword? h) (= "hooks" (namespace h))))

(defn- resolve-path [hooks h]
  {:pre [(keyword? h)]}
  (let [hv (get hooks h)
        p (if (ut/ui-hook? h) (:path hv) hv)]
    (when (nil? p)
      (throw (js/Error. (str "Unable to find mapping for hook " h " in hooks map: " hooks))))

    (if (= "hooks" (namespace (first p)))
      (into (resolve-path hooks (first p)) (vec (rest p)))
      p)))

(defn hook-path [h ctx]
  (let [path (resolve-path (:hooks @state/state) h)]

    (->> (if (ut/ui-hook? h) (conj path :_local-state) path)
         ;replace id's
         (map (fn [p] (get ctx p p)))
         (vec))))


(defn ui-dispatch [{:keys [id hook] :as opts} args]
  (let [{:keys [local-state foreign-states]} (get @component-states id)
        dispatch-f (-> @state/state
                       (get-in [:hooks hook :fns])
                       (get (first args)))]
    (when-not dispatch-f (throw (js/Error. (str "Dispatch function " (first args) " is not defined in hook " hook))))
    ;make ui-update available to dispatch functions
    (apply dispatch-f local-state foreign-states (mk-hook-handle opts) (next args))))


(defn insert-debug-info [result {:keys [options local-state foreign-states]}]
  (let [[start end] (split-at 2 result)
        debug-element [:div {:style {:position   :relative
                                     :align-self :flex-start
                                     :display    :table-cell}}
                       [:button {:class    "debugButton"
                                 :on-click (fn [e]
                                             (println "\n-----------------------")
                                             (println (str "\nHOOK\n" (:hook options)))
                                             (println (str "RENDER RESULT\n" (b-ut/pp-str result)))
                                             (println "OPTIONS\n" (b-ut/pp-str options))
                                             (println "LOCAL-STATE\n" (b-ut/pp-str local-state))
                                             (println "FOREIGN-STATE-KEYS\n" (b-ut/pp-str (keys foreign-states))))}
                        (:hook options)]]]
    (-> (concat start [debug-element] end)
        vec
        (update-in [1 :style] assoc :border "1px solid #00796B"))))

(defn resolve-reactions [reactions-map initial-values]
  (let [initial (fn [v hook] (if v v (hook initial-values)))]
    (reduce-kv (fn [m h r] (assoc m h (initial (deref r) h)))
               {} reactions-map)))

(defn- build-impl
  ([hook] (build-impl hook {}))
  ([hook ctx] (build-impl hook ctx {}))
  ([hook ctx next-ctx] (gui hook (merge ctx next-ctx))))

(defn- mk-hook-handle [options]
  (partial dynamic-api options))

(defn gui [hook ctx]
  {:pre [(keyword? hook) (map? ctx)]}
  (println "NEW GUI - " hook)
  (let [system (state/subscribe [:system] ctx)

        r (get-in @state/state [:hooks hook])
        all-hooks (into [hook] (:reactions r))
        all-paths (reduce (fn [m h] (assoc m h (hook-path h ctx))) {} all-hooks)

        id (hash (get all-paths hook))
        path (get all-paths hook)

        ;should not be reaction dependent
        initial-values (reduce (fn [m h]
                                 (assoc m h (if (ut/ui-hook? h)
                                              (get-in @state/state [:hooks hook :local-state] {})
                                              {})))
                               {}
                               all-hooks)

        ; includes state and foreign state
        reactions-map (reduce (fn [m h] (assoc m h (state/subscribe (h all-paths)))) {} all-hooks)

        options {:id            id
                 :ctx           ctx
                 :path          path
                 :hook          hook
                 :foreign-paths (-> all-paths
                                    (dissoc hook)
                                    vals
                                    vec)}

        f (mk-hook-handle options)]

    (r/create-class
      {:component-did-mount (fn [this] (when-let [dm (:did-mount r)]
                                         (let [{:keys [local-state foreign-states]} (get @component-states id)]
                                           (dm local-state foreign-states f))))
       :reagent-render      (fn [this]
                              (let [{:keys [debug?]} @system
                                    _ (when debug? (println "RENDER - " hook))
                                    ;dereferences and initializes
                                    state-map (resolve-reactions reactions-map initial-values)

                                    local-state (get state-map hook)
                                    foreign-states (dissoc state-map hook)

                                    ;options

                                    _ (swap! component-states assoc id {:options        options
                                                                        :local-state    local-state
                                                                        :foreign-states foreign-states})
                                    render (:render r)]

                                (if-not render
                                  [:div (str "No render: " hook ctx)]

                                  (let [rendered (render local-state foreign-states f) ;instead of reagent calling render function - we do it
                                        [elm elm-opts & remaining] rendered

                                        ;insert dom-id
                                        result (into (if (map? elm-opts)
                                                       [elm (assoc elm-opts :id id)]
                                                       [elm {:id id} elm-opts]) remaining)]



                                    (if debug?
                                      (insert-debug-info result (get @component-states id))
                                      result)))))})))


(defn get-id [ctx hook]
  (hash (hook-path hook ctx)))

(defn- get-handle-data
  ([ctx hook]
   (get-handle-data (get-id ctx hook)))
  ([id]
   (get @component-states id)))

(defn data-and-args [h args]
  (if (hook? (first args))
    {:h-data (get-handle-data (h :ctx) (first args))
     :args   (rest args)}
    {:h-data (get-handle-data (h :id))
     :args   args}))

(defn- update-impl [state {:keys [h-data args]}]
  (let [upd (fn [m] (apply (first args) (if m m (:local-state h-data)) (rest args)))]
    (update-in state (-> h-data :options :path) upd)))


;;;; API


(defn id
  ([h] (h :id))
  ([h sub-id] (str (h :id) sub-id)))

(defn ui-root
  ([hook] (build-impl hook)))

(defn update [state h & args]
  (->> (data-and-args h args)
       (update-impl state)))

(defn put! [h & args]
  (swap! state/state #(apply update % h args)))

(defn get-element
  ([h] (-> h id dom/getElement))
  ([h sub-id] (-> h (id sub-id) dom/getElement)))

(defn local-state [h]
  (-> h id get-handle-data :local-state))

(defn get-handle [ctx hook]
  (-> (get-handle-data ctx hook)
      :options
      mk-hook-handle))

(defn foreign-handle [h foreign-hook]
  (get-handle (h :ctx) foreign-hook))

(defn- dynamic-api [{:keys [id ctx path hook] :as options} & args]
  (let [first-arg (first args)
        second-arg (second args)
        third-arg (first (nnext args))

        h-handle (mk-hook-handle options)]

    ;; update is treated special - it takes state as first arg
    (if (and (map? first-arg) (= second-arg :update))
      (apply update first-arg h-handle (nnext args))
      (condp = first-arg
        :build (build-impl second-arg ctx third-arg)
        ;:update (apply put! h-handle (rest args)) - when called without state it acts as a put!
        :put! (apply put! h-handle (rest args))
        :dispatch (ui-dispatch options (rest args))
        :ctx ctx
        :path path
        :hook hook
        :id (if second-arg (str id second-arg) id)
        :options options
        :else (throw (js/Error. (str "Handle " (first args) " not supported")))))))
