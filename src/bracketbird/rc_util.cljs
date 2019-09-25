(ns bracketbird.rc-util
  (:require [recontain.core :as rc]
            [clojure.string :as string]))


(defn input-handlers [handle]
  {:on-key-down #(rc/dispatch-silent handle :on-key-down %)
   :on-key-up   #(rc/dispatch-silent handle :on-key-up %)
   :on-blur     #(rc/dispatch-silent handle :on-blur %)
   :on-focus    #(rc/dispatch-silent handle :on-focus %)
   :on-change   #(rc/dispatch-silent handle :on-change %)})


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



