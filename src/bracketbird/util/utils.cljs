(ns bracketbird.util.utils
  [:require [bracketbird.model.entity :as ie]
            [bracketbird.context :as context]
            [goog.dom :as dom]

            [bracketbird.util.position :as pos]])


(defn entity [entities e-id]
  (some (fn [e] (when (= e-id (:entity-id e)) e)) entities))

(defn index-of-entity-id
  "Returns [index-of-entity entity]"
  [entities e-id]
  (first (keep-indexed (fn [i e] (when (= e-id (ie/-id e)) i)) entities)))

(defn remove-entity-by-id [entities e-id]
  (remove (fn [e] (= (ie/-id e) e-id)) entities))

(defn next-entity [entities entity]
  (let [index (index-of-entity-id entities (ie/-id entity))]
    (when index (get (vec entities) (inc index)))))

(defn previous-entity [entities entity]
  (let [index (index-of-entity-id entities (ie/-id entity))]
    (when index (get (vec entities) (dec index)))))

(defn r-key [entity r-form]
  (with-meta r-form {:key (hash (ie/-id entity))}))

(defn dom-id-from-entity [entity sub-key]
  (str (hash [(ie/-id entity) sub-key])))

(defn dom-id-from-ui-ctx [ctx sub-key]
  (str (hash (str (context/ui-path ctx) sub-key))))

(defn focus-by-node [dom-node]
  (when dom-node
    ;(pos/scroll-into-view dom-node (dom/getElement panel-id))
    (.focus dom-node)
    true))

(defn focus-by-id [dom-id]
  (-> dom-id
      str
      dom/getElement
      focus-by-node))

(defn focus-by-entity [entity sub-key]
  (when entity
    (-> entity (dom-id-from-entity sub-key) focus-by-id)))

(defn focus-by-ui-ctx [ctx sub-key]
  (-> ctx (dom-id-from-ui-ctx sub-key) focus-by-id))