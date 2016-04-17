(ns bracketbird.contexts.context
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [bracketbird.app-state :as app-state]))


(defprotocol IBusiness-event
  (-execute-business [this data]))

(defprotocol IService-event
  (-execute-service! [this data])
  (-handle-service-result [this data service-result]))

(defprotocol IContext
  (-state [this])
  (-root-path [this])
  (-id [this])
  (-load [this])
  (-unload [this]))


;------- util functions ------
(defn validate [ctx]
  {:pre [(satisfies? IContext ctx)
         (satisfies? reagent.ratom/IReactiveAtom (-state ctx))
         ((some-fn vector? nil?) (-root-path ctx))
         ((some-fn keyword? nil?) (-id ctx))]}
  ctx)

(defn as-string [ctx]
  (apply str (-root-path ctx)))

(defn context-path [ctx]
  (into (if (-root-path ctx) (-root-path ctx) []) (if (-id ctx) [(-id ctx)] nil)))

(defn ui-state-path [ctx]
  (into (context-path ctx) [:ui-state]))

(defn data-path [ctx]
  (into (context-path ctx) [:data]))

(defn get-data [ctx]
  (get-in @(-state ctx) (data-path ctx) {}))

(defn get-in-ctx [ctx path]
  (get-in @(-state ctx) (concat path (data-path ctx)) {}))

(defn swap-data! [ctx data]
  (swap! (-state ctx) assoc-in (data-path ctx) data))

(defn get-root [ctx]
  (get-in @(-state ctx) (-root-path ctx)))

(defn swap-root! [ctx root]
  (swap! (-state ctx) assoc-in (-root-path ctx) root))

(defn get-ui-state! [ctx]
  (get-in @(-state ctx) (ui-state-path ctx) {}))

(defn swap-ui-state! [ctx new-ui-state]
  (swap! (-state ctx) assoc-in (ui-state-path ctx) new-ui-state))

(defn delete-ui-state! [ctx]
  (swap-ui-state! ctx {}))

(defn remove-context! [ctx]
  (->> (dissoc (get-root ctx) (-id ctx))
       (swap-root! ctx)))

;------- core api functions ---------

(defn ui-state [ctx path]
  {:pre [(satisfies? IContext ctx) (vector? path)]}
  (r/cursor (-state ctx) (into (ui-state-path ctx) path)))

(defn subscribe [ctx path]
  {:pre [(satisfies? IContext ctx) (vector? path)]}
  (reaction (get-in @(-state ctx) (into (data-path ctx) path))))

(defn- dispatch-business-event [ctx event]
  (->> (get-data ctx)
       (-execute-business event)
       (swap-data! ctx)))

(defn- dispatch-service-event [ctx event]
  (-> (-execute-service! event (get-data ctx))
      #_(p/then (fn [v] (swap-data! ctx (-handle-service-result event (get-data ctx) v))))))

(defn dispatch [ctx event]
  {:pre [(satisfies? IContext ctx)]}
  (cond
    (satisfies? IBusiness-event event)
    (dispatch-business-event ctx event)

    (satisfies? IService-event event)
    (dispatch-service-event ctx event)

    :else (throw (js/Error. "Unable to dispatch event: " event))))


;-------- global singleton context ----

(def Application-context
  (reify
    IContext

    (-state [this] app-state/state)
    (-id [this])
    (-root-path [this])
    (-load [this])
    (-unload [this])))


(defn subscriber-user []
  (subscribe Application-context [:user]))

(defn subscribe-config []
  (subscribe Application-context [:configuration]))
