(ns bracketbird.rc-util
  (:require [recontain.core :as rc]
            [clojure.string :as string]
            [bracketbird.util :as ut]
            [bracketbird.dom :as d]))

(defn- bound-name [sub-id value-name]
  (let [resolved-name (if (keyword? value-name) (name value-name) value-name)]
    (keyword (if-not (string/blank? sub-id)
               (str sub-id "-" resolved-name)
               resolved-name))))

(defn- put-value [h sub-id k v]
  (rc/put! h assoc (bound-name sub-id k) v))

(def event-handler-fns {:on-focus       (fn [h sub-id ls e]
                                          (put-value h sub-id "focus?" true))

                        :on-blur        (fn [h sub-id ls e]
                                          (put-value h sub-id "focus?" false))

                        :on-mouse-enter (fn [h sub-id ls e]
                                          (put-value h sub-id "hover?" true))

                        :on-mouse-leave (fn [h sub-id ls e]
                                          (put-value h sub-id "hover?" false))

                        :on-key-down    (fn [h sub-id ls e]
                                          (let [dob (bound-name sub-id "delete-on-backspace?")]
                                            (d/handle-key e {[:BACKSPACE]
                                                             (fn [_]
                                                               (when (get ls dob)
                                                                 (rc/dispatch-silent h (bound-name sub-id "delete-on-backspace"))) [:STOP-PROPAGATION])})))
                        :on-key-up      (fn [h sub-id _ e]
                                          (when (= "text" (.-type (.-target e)))
                                            (put-value h sub-id "delete-on-backspace?" (clojure.string/blank? (ut/value e)))))

                        :on-change      (fn [h sub-id ls e]
                                          (put-value h sub-id "value" (ut/value e)))

                        :on-click       (fn [_ _ _ _] ())})

(def events-shorts->event-handlers {:FOCUS  [:on-focus :on-blur]
                                    :HOVER  [:on-mouse-enter :on-mouse-leave]
                                    :CHANGE [:on-change]
                                    :CLICK  [:on-click]
                                    :KEY    [:on-key-down :on-key-up]})


(defn bind-events
  [opts h events]
  (let [ls (rc/get-local-state (:id h))
        [_ sub-id] (string/split (:id opts) "#")]
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
                                  (f h sub-id ls e)
                                  (rc/dispatch-silent h (bound-name sub-id k) e)))))
                 opts))))

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



