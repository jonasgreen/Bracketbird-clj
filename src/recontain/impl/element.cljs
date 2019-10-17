(ns recontain.impl.element
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [recontain.impl.state :as rc-state]
            [bracketbird.state :as state]))

(defn- namespaced?
  ([item]
   (and (keyword? item) (namespace item)))

  ([item str-namespace]
   (and (keyword? item)
        (= str-namespace (namespace item)))))

(defn element-form? [form]
  (when (vector? form)
    (let [first-item (first form)
          second-item (second form)]
      ;[::team-name :e/text-input ...]
      (and (namespaced? first-item)
           (namespaced? second-item "e")))))

(defn merge-configs [configs])

(defn decorate-element [form {:keys [parent-dom-id parent-path parent-configs]}]
  (-> (into [@rc-state/element-fn {:element-id     (second form)
                                   :parent-path    parent-path
                                   :parent-dom-id      parent-dom-id
                                   :parent-configs parent-configs}] (subvec form 2))
      (with-meta (meta form))))

(defn mk-element
  [{:keys [element-id parent-dom-id parent-path parent-configs]} org-value]
  (let [path (-> parent-configs drop-last vec)
        local-state-atom (reaction (get-in state/state path))
        dom-id (rc-state/dom-element-id parent-dom-id element-id)

        _ (println "element-id" element-id)
        _ (println "parent-dom-id" parent-dom-id)
        _ (println "parent-path" parent-path)
        _ (println "parent-configs" parent-configs)
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
                                 (let [config (->> element-id
                                                   (get @rc-state/elements-configurations)
                                                   (conj parent-configs))]

                                   [:div (str element-id)]))})))
