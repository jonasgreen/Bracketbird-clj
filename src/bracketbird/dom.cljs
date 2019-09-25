(ns bracketbird.dom
  (:require [clojure.set :as s]
            [goog.dom :as dom-helper])

  (:import [goog.events EventType]
           [goog.events KeyCodes]))

(defonce key-to-codes (js->clj goog.events.KeyCodes :keywordize-keys true))
(defonce codes-to-keys (s/map-invert key-to-codes))

(defonce event-types (js->clj goog.events.EventType :keywordize-keys true))
(defonce types-to-keys (s/map-invert event-types))



; focus
; ---------------

(defn focus-by-node [dom-node]
  (when dom-node
    ;(pos/scroll-into-view dom-node (dom/getElement panel-id))
    (.focus dom-node)
    true))

(defn focus-by-id [dom-id]
  (-> dom-id
      str
      dom-helper/getElement
      focus-by-node))


; Navigating
; ----------------

(def navigators {:parent           dom-helper/getParentElement
                 :first-child      dom-helper/getFirstElementChild
                 :last-child       dom-helper/getLastElementChild
                 :previous-sibling dom-helper/getPreviousElementSibling
                 :next-sibling     dom-helper/getNextElementSibling})

(defn navigate-dom
  "Short and nil risistant way to navigate dom. Takes a node as starting point and a vector of navigator keys like this:
  (navigate-dom node [:parent :next-sibling :last-child])
  Returns nil if any navigation is not possible"
  [start-node paths]
  (loop [node start-node ps paths]
    (if (or (not (seq ps)) (not node))
      node
      (do (let [f (get navigators (first ps))]
            (if f
              (recur (f node) (rest ps))
              ((throw (js/Error. (str (first ps) " is not supported as path"))))))))))




; Stylesheet
; ------------

(defn get-style-sheet [s-name]
  (let [length (-> js/document (.-styleSheets) (.-length))
        sheets (-> js/document (.-styleSheets))]

    (for [i (vec (range length))
          ;sheet (aget sheets i)
          :when (= s-name (.-title (aget sheets i)))] (aget sheets i))))

(defn mk-stylesheet [unique-name]
  (let [sheet (-> js/document
                  (.createElement "style"))]

    (aset sheet "title" unique-name)
    (.info js/console sheet)

    (-> js/document
        (.-body)
        (.appendChild sheet))))


; Browser local storage
; -----------------------

(defn local-storage-write [id data]
  (-> (.-localStorage js/window)
      (.setItem id (->
                     (clj->js data)
                     js/JSON.stringify))))

(defn local-storage [id]
  (-> (.-localStorage js/window)
      (.getItem id)
      js/JSON.parse
      (js->clj :keywordize-keys true)))

(defn local-storage-remove [id]
  (-> (.-localStorage js/window)
      (.removeItem id)))


; Position
; ----------

(defn absolut-position
  "Returns a map containing the aboslut left, top, width, height of
  the dom-element."
  [node]
  (let [p (.getBoundingClientRect node)
        left (.-left p)
        width (.-width p)
        height (.-height p)
        top (.-top p)

        w_height (.-innerHeight js/window)
        w_width (.-innerWidth js/window)]

    {:left          left :top top :right (- w_width (+ left width)) :bottom (- w_height (+ top height))
     :width         width :height height
     :window-width  w_width
     :window-height w_height}))


(defn offset-position
  "Returns a map containing the offset left, top, width, height of
  the dom-element."
  [node]
  {:left (.-offsetLeft node) :top (.-offsetTop node) :width (.-offsetWidth node) :height (.-offsetHeight node)})


(defn above?
  "returns true if postion p1's top is above postition p2's top."
  [p1 p2]
  (< (:top p1) (:top p2)))

(defn below?
  "returns true if postion p1's 'bottom' is below postition p2's 'bottom'."
  [p1 p2]
  (> (+ (:height p1) (:top p1)) (+ (:height p2) (:top p2))))


(defn scroll-into-view
  "Makes sure an item is visible in a scroll panel.

  item-node - the dom node that should be visible.
  panel-node - the surrounding scroll panel dom node."

  [item-node panel-node]
  (try
    (let [p-item (absolut-position item-node)
          p-panel (absolut-position panel-node)]

      (cond
        (above? p-item p-panel) (.scrollIntoView item-node true)
        (below? p-item p-panel) (.scrollIntoView item-node false)))
    (catch :default e (throw (js/Error. (str "unable to scroll into view - ref-item:" item-node "panel-ref:" panel-node))))))


; Keys
; -------------


(defn shift-modifier? [event]
  (when event (.-shiftKey event)))

(defn ctrl-modifier? [event]
  (when event (.-ctrlKey event)))

(defn alt-modifier? [event]
  (when event (.-altKey event)))

(defn meta-modifier? [event]
  (when event (.-metaKey event)))


(defn modifier? [event]
  (or (shift-modifier? event) (ctrl-modifier? event) (alt-modifier? event) (meta-modifier? event)))

(defn not-modifier? [event]
  (-> event modifier? not))

(defn key-and-modifier? [k m-pred event]
  (and (= (k key-to-codes) (.-keyCode event))
       (m-pred event)))

(defn no-modifiers? [event]
  (-> event modifier? not))

(defn key? [k event]
  (key-and-modifier? k (comp not modifier?) event))


(defn key-handler [fns]
  (let [{:keys [else]} fns
        modifier-preds {:SHIFT shift-modifier?
                        :ALT   alt-modifier?
                        :CTRL  ctrl-modifier?
                        :META  meta-modifier?}

        exits-fns {:STOP-PROPAGATION (fn [e] (.stopPropagation e))
                   :PREVENT-DEFAULT  (fn [e] (.preventDefault e))}

        fns-by-set (reduce-kv (fn [m k v] (assoc m (set (if (sequential? k) k [k])) v)) {} (dissoc fns :else))]
    (fn [e]
      ; produce a key set from event
      (let [key-set (reduce-kv (fn [s k v] (if (v e) (conj s k) s))
                               #{(get codes-to-keys (.-keyCode e))}
                               modifier-preds)

            ;find function from fns-map by key-set
            f (get fns-by-set key-set else)

            ;expects exits to be in the form [:STOP-PROPAGATION :PREVENT-DEFAULT]
            exits (when f (f e))]

        (when (sequential? exits)
          (doall (->> exits
                      (map exits-fns)
                      (map (fn [exit-f] (when exit-f (exit-f e)))))))))))

(defn handle-key [e fns]
  ((key-handler fns) e))



; Reagent
; -------------

(defn- as-options [opts]
  (if (or (nil? opts) (not (map? opts)))
    {}
    opts))

(defn- as-children [r-args]
  (if (map? (first r-args))
    (rest r-args)
    r-args))

(defn- r-comp [r-element r-args style]
  "Use it when creating functions that creates flexible generic reagent-components that has
  the same api as normal reagent components.

  Parameters:
  :r-element    - reagent/html element type like :div :span :p :ul etc.
  :r-args       - reagent arguments, which is a sequence of an optional map of options followed
                  by zero, one or multiple children.
  :style        - default style of the component..

  Example: You want to create an api function for creating a specific kind of flex components
           do like this:
              (defn space-between [r-args]
                [util/r-comp :div r-args {:display :flex :justify-content :space-between}])

           The api function could now be called like this:
              [space-between {:style {:background :red}} [div back] [div forward]]

           Or like this:
              [space-between back forward]

           Or like this:
              [space-between (map-indexed (fn[i t] ^{:key id}[:div t]) [back forward]])] "

  (apply conj [r-element (merge-with merge {:style style} (as-options (first r-args)))] (as-children r-args)))
