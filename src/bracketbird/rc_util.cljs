(ns bracketbird.rc-util
  (:require [recontain.core :as rc]
            [clojure.string :as string]
            [bracketbird.util :as ut]))


(defn- put-value [h sub-id k v]
  (rc/put! h assoc (keyword (if-not (string/blank? sub-id)
                              (str sub-id "-" k)
                              k)) v))

(def event-fns {:on-focus       (fn [h sub-id e] (put-value h sub-id "focus?" true))
                :on-blur        (fn [h sub-id e] (put-value h sub-id "focus?" false))

                :on-mouse-enter (fn [h sub-id e] (put-value h sub-id "hover?" true))
                :on-mouse-leave (fn [h sub-id e] (put-value h sub-id "hover?" false))

                :on-key-down    (fn [_ _ _] ())
                :on-key-up      (fn [_ _ _] ())

                :on-change      (fn [h sub-id e] (put-value h sub-id "value" (ut/value e)))
                :on-click       (fn [_ _ _] ())})

(def standard-bind-events [:on-focus :on-blur :on-mouse-enter :on-mouse-leave :on-key-down :on-key-up :on-change :on-click])

(defn bind-events
  ([opts h]
   (bind-events opts h standard-bind-events))

  ([opts h events]
  (let [[_ sub-id] (string/split (:id opts) "#")]
    (reduce (fn [m k]
              (when-let [f (get event-fns k)]
                (assoc m k (fn [e]
                             (f h sub-id e)
                             (rc/dispatch-silent h (keyword (if-not (string/blank? sub-id)
                                                              (str sub-id "-" (name k))
                                                              k)) e)))))
            opts
            events))))

(defn focus
  ([handle hook ctx-id ctx-value]
   (when-let [ctx-value (if (map? ctx-value) (get ctx-value ctx-id) ctx-value)]
     (focus handle hook {ctx-id ctx-value})))

  ([handle hook extra-ctx]
   (-> (:ctx handle)
       (merge extra-ctx)
       (rc/get-handle hook)
       (rc/dispatch :focus)))

  ([handle hook]
   (focus handle hook {})))

(defn has-changed [value org-value]
  (when value
    (if (and (string? value) (string? org-value))
      (if (and (string/blank? value)
               (string/blank? org-value))
        false
        (not= value org-value))
      (not= value org-value))))



