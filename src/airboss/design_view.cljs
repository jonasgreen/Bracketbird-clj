(ns airboss.design-view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [reagent.core :as r]
            [airboss.utils :as ut])
  (:import [goog.events EventType]
           [goog.events KeyCodes]))
;--------
; styles
;--------


(def text-style
  {:-webkit-user-select "none"
   :-moz-user-select    "none"
   :padding             4
   :color               "white"
   :font-size           12})

(def x-y-label-style
  (merge text-style {:font-size 10
                     :position  :absolute}))

(def design-view-style {:position    :absolute
                        :will-change :transform
                        :background  "rgba(0,0,0,0.4)"
                        :z-index     200000
                        :right       0
                        :top         0
                        :width       "100vw"
                        :height      "100vh"})

;--------
; state
;--------

(defonce state (r/atom nil))

(def local-storage-id "airboss-design-viewer")

;-------
; utils
;-------

(defn round [n]
  (/ (js/Math.round (* n 10)) 10))

(defn get-id [p]
  (let [id (:id p)]
    (if (keyword? id)
      id
      (keyword id))))

(defn point [x y]
  {:id (keyword (str (gensym) (.getTime (js/Date.)))) :x x :y y})

(defn add-point [points-map p]
  (assoc points-map (get-id p) p))

;------------------
; dragging actions
;------------------

(defn drag-move-handler [on-drag]
  (fn [evt]
    (on-drag (.-clientX evt) (.-clientY evt))))

(defn drag-end-handler [drag-move drag-end on-end]
  (fn [evt]
    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
    (events/unlisten js/window EventType.MOUSEUP @drag-end)
    (on-end)))

(defn dragging [{:keys [on-drag on-start on-end]
                 :or   {on-start (fn []) on-end (fn [])}}]
  (let [drag-move (drag-move-handler on-drag)
        drag-end-atom (atom nil)
        drag-end (drag-end-handler drag-move drag-end-atom on-end)]
    (on-start)
    (reset! drag-end-atom drag-end)
    (events/listen js/window EventType.MOUSEMOVE drag-move)
    (events/listen js/window EventType.MOUSEUP drag-end)))

(defn move-point [p]
  (fn [x y]
    (swap! state update-in [:points (get-id p)] assoc :x x :y y)))

;-------------
; ui-elements
;-------------

(defn svg-line [p1 p2 color]
  [:line {:stroke color :stroke-width 1 :x1 (:x p1) :y1 (:y p1) :x2 (:x p2) :y2 (:y p2)}])

(defn intersection-layer [points color {:keys [width height]}]
  [:g (map-indexed (fn [i p]
                     ^{:key i} [:g
                                [svg-line (point 0 (:y p)) (point (+ (:x p) width) (:y p)) color]
                                [svg-line (point (:x p) 0) (point (:x p) (+ (:y p) height)) color]]) points)])

(defn triangle-hubs [{:keys [width height r-w r-h]}]
  (let [y1 (- height r-h)
        y2 r-h
        x1 (- width r-w)
        x2 r-w
        h 6.5
        s {:fill "yellow"}]
    [:g
     ;left
     [:polygon {:style s :points (str "0," (- y1 h) " 4," y1 " 0," (+ y1 h))}]
     [:polygon {:style s :points (str "0," (- y2 h) " 4," y2 " 0," (+ y2 h))}]

     ;right
     [:polygon {:style s :points (str width "," (- y1 h) " " width "," (+ y1 h) " " (- width 4) "," y1)}]
     [:polygon {:style s :points (str width "," (- y2 h) " " width "," (+ y2 h) " " (- width 4) "," y2)}]

     ;top
     [:polygon {:style s :points (str (- x1 h) ",0 " (+ x1 h) ",0 " x1 "," 4)}]
     [:polygon {:style s :points (str (- x2 h) ",0 " (+ x2 h) ",0 " x2 "," 4)}]

     ;bottom
     [:polygon {:style s :points (str (- x1 h) "," height " " (+ x1 h) "," height " " x1 "," (- height 4))}]
     [:polygon {:style s :points (str (- x2 h) "," height " " (+ x2 h) "," height " " x2 "," (- height 4))}]]))


(defn key-down [e p move]
  (let [dist (if (.-shiftKey e) 25 1)]
    (cond
      (= KeyCodes.LEFT (.-keyCode e))
      (move (- (:x p) dist) (:y p))

      (= KeyCodes.RIGHT (.-keyCode e))
      (move (+ (:x p) dist) (:y p))

      (= KeyCodes.UP (.-keyCode e))
      (move (:x p) (- (:y p) dist))

      (= KeyCodes.DOWN (.-keyCode e))
      (move (:x p) (+ (:y p) dist)))))

(defn handle [{:keys [on-drag color]} p focus-id]
  [:g {:id            (str (get-id p))
       :style         {:cursor :pointer}
       :tabIndex      0
       :on-focus      (fn [e]
                        (swap! state assoc :focus-id (get-id p)))
       :on-mouse-down #(dragging {:on-drag on-drag})
       :on-key-down   #(key-down % p on-drag)}

   [:circle {:stroke       (if (= (get-id p) focus-id) "rgba(255,255,255, 0.5)" "transparent")
             :stroke-width 5
             :r            4
             :fill         color
             :cx           (:x p)
             :cy           (:y p)}]

   [:circle {:stroke       "yellow"
             :stroke-width 2
             :r            10
             :fill         "transparent"
             :cx           (:x p)
             :cy           (:y p)}]
   ])

(defn handles-layer [points focus-id]
  [:g (map (fn [p] ^{:key (get-id p)} [handle {:on-drag (move-point p) :color "yellow"} p focus-id]) points)])

(defn x-labels [points width]
  [:div (map (fn [p] ^{:key (get-id p)} [:div {:style (merge x-y-label-style
                                                             {:left   (if (> (+ 30 (:x p)) width)
                                                                        (- (:x p) 30)
                                                                        (+ (:x p) 4))
                                                              :bottom 2})} (round (:x p))]) points)])

(defn y-labels [points height]
  [:div (map (fn [p] ^{:key (get-id p)} [:div {:style (merge x-y-label-style
                                                             {:left 4
                                                              :top  (if (> (+ 30 (:y p)) height)
                                                                      (- (:y p) 30)
                                                                      (+ (:y p) 4))})} (round (:y p))]) points)])

(defn- get-dimensions []
  {:width  (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :r-w    (/ (.-innerWidth js/window) 1.618033)
   :r-h    (/ (.-innerHeight js/window) 1.618033)})

(defn init-data []
  (let [d (get-dimensions)]
    {:points     (-> {}
                     (add-point (point (- (:width d) (:r-w d)) (- (:height d) (:r-h d))))
                     (add-point (point (:r-w d) (:r-h d))))
     :dimensions (get-dimensions)}))

(defn- key-down-handler [e]
  (cond
    (= KeyCodes.R (.-keyCode e))
    (do
      (ut/local-storage-remove local-storage-id)
      (reset! state (init-data)))

    (= KeyCodes.F (.-keyCode e))
    (do
      (let [ps (vals (:points @state))
            first-id (get-id (first ps))
            second-id (get-id (second ps))
            f-id (:focus-id @state)]
        (.focus (dom/getElement (str (if (= f-id first-id) second-id first-id))))))))

(defn- resize-handler [e]
  (swap! state assoc :dimensions (get-dimensions)))

(defn render []
  (let [local-state (ut/local-storage local-storage-id)]
    (reset! state (if (seq local-state) local-state (init-data))))


  (r/create-class
    {:reagent-render         (fn []
                               (let [points (vals @(reaction (:points @state)))
                                     dimensions @(reaction (:dimensions @state))
                                     focus-id @(reaction (:focus-id @state))]
                                 [:div {:style design-view-style}

                                  ;svg
                                  [:svg {:style {} :width "100%" :height "100%"}
                                   [intersection-layer points "white" dimensions]
                                   [triangle-hubs dimensions]
                                   [handles-layer points focus-id]]

                                  [x-labels points (:width dimensions)]
                                  [y-labels points (:height dimensions)]]))

     :component-did-mount    (fn [_]
                               (events/listen js/window EventType.KEYDOWN key-down-handler)
                               (events/listen js/window EventType.RESIZE resize-handler)
                               (swap! state assoc :dimensions (get-dimensions))
                               (let [focus-id (:focus-id @state)
                                     r-id (if (nil? focus-id)
                                            (get-id (first (vals (:points @state))))
                                            (if (keyword? focus-id) focus-id (keyword focus-id)))]
                                 (.focus (dom/getElement (str r-id)))))

     :component-will-unmount (fn [_]
                               (ut/local-storage-write local-storage-id @state)
                               (events/unlisten js/window EventType.KEYDOWN key-down-handler)
                               (events/unlisten js/window EventType.RESIZE resize-handler))}))


(defn- ui-toggler []
  (let [visible (r/atom false)]

    (events/listen js/window "keydown"
                   (fn [e] (cond
                             (ut/key? :F10 e) (do (.preventDefault e) (reset! visible (not @visible)))
                             (ut/key? :ESC e) (do (.preventDefault e) (reset! visible false)))))

    (fn [] (when @visible [render]))))

(defn load []
  (r/render-component [ui-toggler] (-> (.-body js/document)
                                                      (.appendChild (dom/createElement "div")))))
