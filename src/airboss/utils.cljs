(ns airboss.utils

  (:import [goog.events EventType]
           [goog.events KeyCodes]))

(defonce key-codes (js->clj goog.events.KeyCodes :keywordize-keys true))

;------------
; Stylesheet
;------------

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


;-----------------------
; Browser local storage
;-----------------------

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


;----------
; position
;----------

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


; ------- key utils ----

(defn key? [k event]
  (= (k key-codes) (.-keyCode event)))

(defn shift-modifier? [event]
  (when event (.-shiftKey event)))

(defn ctrl-modifier? [event]
  (when event (.-ctrlKey event)))

(defn alt-modifier? [event]
  (when event (.-altKey event)))

(defn meta-modifier? [event]
  (when event (.-metaKey event)))

(defn modifier? [event]
  (or (shift-modifier? event)(ctrl-modifier? event)(alt-modifier? event)(meta-modifier? event)))


;-----------------------

(defn index-of
  ([item coll] (index-of 0 item coll =))

  ([item coll condition-fn] (index-of 0 item coll condition-fn))

  ([index item coll condition-fn]
   (cond
     (empty? coll) -1
     (condition-fn item (first coll)) index
     :else (recur (inc index) item (rest coll) condition-fn))))


(defn cyclic-previous [item coll]
  (cond
    (nil? item) (last coll)
    (= item (first coll)) (last coll)
    :else (nth coll (dec (index-of item coll)))))

(defn cyclic-next [item coll]
  (cond
    (nil? item) (first coll)
    (= item (last coll)) (first coll)
    :else (nth coll (inc (index-of item coll)))))
