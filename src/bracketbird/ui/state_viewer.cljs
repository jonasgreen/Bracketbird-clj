(ns bracketbird.ui.state-viewer
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [goog.dom :as dom-helper]
            [bracketbird.util.keyboard :as k]
            [bracketbird.util.position :as pos]
            [bracketbird.util.dom-util :as util])
  (:import [goog]))

(def tab-size 200)
(def panel-id "atom_state_viewer")

(defonce initial-state (r/atom {:last-focus nil}))

(defonce local-state-atom (r/atom {:filter    ""
                                   :open-rows #{[]}}))

;---------
; control
;---------

(defn- id [path]
  (str path))

(defn- container? [value]
  (or (seqable? value)
      (satisfies? IAtom value)))

(defn- get-keys [value]
  (cond
    (map? value) (sort-by str (keys value))
    (sequential? value) (sort (mapv identity (range (count value))))
    :else []))

(defn- open? [state-atom path]
  (contains? (:open-rows @state-atom) path))

(defn- close-row [state-atom path]
  ;dont remove root - should always be visible
  (when-not (= [] path)
    (swap! state-atom update-in [:open-rows] disj path)))

(defn- focus [node]
  (when node
    (pos/scroll-into-view node (dom-helper/getElement panel-id))
    (.focus node)))

(defn- navigate-to [e node route]
  (do (.preventDefault e)
      (focus (util/navigate-dom node route))))

(defn- raw-data [data]
  (cond
    (satisfies? IAtom data) @data
    (= LazySeq (type data)) (vec data)
    (set? data) (vec data)
    :else data))

(defn- get-data [path data]
  (loop [p path d (raw-data data)]
    (if (some nil? [(seq p) d])
      d
      (recur (rest p) (get (raw-data d) (first p))))))

(defn- contains-string [filter s]
  (let [f (re-find (js/RegExp filter "i") s)]
    (not (clojure.string/blank? f))))

(defn- build-paths
  "Recursively (top-down) builds a vector of paths to all values. Stops when value is not an 'open container'"
  ([state-atom value]
   (subvec (build-paths state-atom [] value []) 1))

  ([state-atom path value all-paths]
   (let [paths (conj all-paths path)
         raw-value (raw-data value)]
     (if (open? state-atom path)
       (doall (reduce (fn [rs k]
                        (build-paths state-atom (conj path k) (get raw-value k) rs)) paths (get-keys raw-value)))
       paths))))

(defn- build-paths-from-filter
  "Recursively (bottom-up) builds a vector of paths to all values that fits filter."
  ([value filter]
   (let [raw-value (raw-data value)
         paths (reduce (fn [v k] (let [path (build-paths-from-filter k (get raw-value k) filter)]
                                   (if path
                                     (into v path) v))) [] (get-keys raw-value))]
     ;also build paths of parents
     (rest (distinct (reduce (fn [v p] (into v (reduce (fn [v1 p1] (conj v1 (into (last v1) [p1]))) [[]] p))) [] paths)))))

  ([k value filter]
   (let [raw-value (raw-data value)]
     (if (container? value)
       (reduce (fn [v k1] (let [paths (build-paths-from-filter k1 (get raw-value k1) filter)]
                            ;take keys/paths that qualifies by filter and put k (key) in front of them
                            (reduce (fn [v1 p] (conj v1 (into [k] p))) v paths))) [] (get-keys raw-value))
       (when (contains-string filter (str k))
         [[k]])))))

;--------------
; search-field
;--------------

(defn- handle-search-key-down [r-comp on-change e]
  (cond
    (and (k/arrow-down? e) (not (k/shift-modifyer? e))) (navigate-to e (r/dom-node r-comp) [:next-sibling])
    (k/enter? e) (on-change (.-value (.-target e)))))

(defn- search-field [value on-change]
  (let [state (r/atom {:filter value})]
    (fn [value on-change]
      [:input {:id        "state_viewer_search_field"
               :class     "state-view_search"
               :onFocus   #(swap! initial-state assoc :last-focus "state_viewer_search_field")
               :onChange  (fn [e] (let [v (.-value (.-target e))]
                                    (swap! state assoc :filter v)
                                    (when (clojure.string/blank? v) (on-change ""))))
               :value     (:filter @state)
               :onKeyDown (partial handle-search-key-down (r/current-component) on-change)}])))

;--------------
; render value
;--------------

(defn- render-value-empty-container [value]
  (cond
    (map? value) "{ }"
    (list? value) "( )"
    (vector? value) "[ ]"
    (set? value) "#{ }"
    (= LazySeq (type value)) "( ) Lazy seq"
    :default "[[[]]]"))

(defn- render-value-container-with-children [value]
  (cond
    (map? value) (str "{+} " (count value))
    (list? value) (str "(+) " (count value))
    (vector? value) (str "[+] " (count value))
    (set? value) (str "#{+} " (count value))
    (= LazySeq (type value)) (str "(+) " (count value) " Lazy seq")
    :default "[[[+]]]"))

(defn- render-value-container [state-atom path value filter]
  (if (seq value)
    (if (or (open? state-atom path) (not (clojure.string/blank? filter)))
      ""
      (render-value-container-with-children value))
    (render-value-empty-container value)))

(defn- render-value [state-atom path value filter]
  (if (container? value)
    (render-value-container state-atom path value filter)
    (if value (str value) "nil")))

;------------
; render row
;------------

(defn- handle-row-key-down [state-atom path value r-comp e]
  (cond
    (k/arrow-left? e) (if (and
                            (container? value)
                            (open? state-atom path))
                        (close-row state-atom path)
                        (do
                          (close-row state-atom (vec (drop-last path)))
                          (focus (dom-helper/getElement (id (vec (drop-last path)))))))
    (k/arrow-right? e) (when
                         (container? value)
                         (swap! state-atom update-in [:open-rows] conj path))

    (k/arrow-up? e) (navigate-to e (r/dom-node r-comp) [:previous-sibling])
    (k/arrow-down? e) (navigate-to e (r/dom-node r-comp) [:next-sibling])
    (k/delete? e) ()
    (k/enter? e) ()))


(defn- render-key [key-value]
  (if (keyword? key-value)
    (let [key-ns (namespace key-value)
          key-name (name key-value)]
      (if key-ns
        [:div
         [:span {:style {:color :lightgrey :opacity 0.5}} (str ":" key-ns) "/"] [:span (str key-name)]]
        [:span (str key-value)]
        ))

    (if key-value [:span (str key-value)] "nil")))


(defn- render-row [state-atom path value filter]
  (let [atom? (satisfies? IAtom value)
        v (if atom? @value value)]
    ;row
    [:div {:id        (id path)
           :class     "state-view_row"
           :tab-index 0
           :style     {:width        "100%"
                       :display      :flex
                       :line-height  1
                       :padding      6
                       :align-items  :flex-end
                       :padding-left (+ 4 (* tab-size (- (count path) 1)))}
           :onFocus   #(swap! initial-state assoc :last-focus path)
           :onKeyDown (partial handle-row-key-down state-atom path v (r/current-component))}
     ;key
     [:div {:style {:min-width tab-size}}
      (if atom? (str (last path) " @atom") (render-key (last path)))]
     ;value
     [:div {:style {:min-width     tab-size
                    :white-space   :nowrap
                    :overflow      :hidden
                    :text-overflow :ellipsis}}
      (render-value state-atom path v filter)]]))

(defn- render-ui [data filter on-filter-change]
  (let [paths (if (clojure.string/blank? filter)
                (build-paths local-state-atom data)
                (build-paths-from-filter data filter))]
    [:div {:id panel-id :class "state-view" :style {:position :absolute :right 0 :top 0}}
     [search-field filter on-filter-change]
     (doall (map (fn [p] ^{:key (id p)} [render-row local-state-atom p (get-data p data) filter]) paths))]))

(defn render
  "Creates a panel that shows the data in the given data-atom. The panel should only be used as a singleton on
  application level - if multiple instances are shown - it will fail"
  [_]
  (r/create-class
    {:reagent-render         (fn [data-atom]
                               [render-ui @data-atom (:filter @local-state-atom) (fn [v] (swap! local-state-atom assoc :filter v))])

     :component-did-mount    (fn [r-comp]
                               (let [last-focus (:last-focus @initial-state)
                                     last-node (dom-helper/getElement (id last-focus))
                                     next (if last-node last-node (util/navigate-dom (r/dom-node r-comp) [:first-child]))]
                                 (when next (.focus next))))
     :component-will-unmount (fn [_])}))

