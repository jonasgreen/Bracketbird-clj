(ns airboss.state-view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [airboss.utils :as ut]
            [reagent.core :as r]
            [reagent.ratom :as r-atom]
            [clojure.string :as s]
            [goog.dom :as dom-helper]
            [goog.events :as events]
            [airboss.state-editor :as editor]
            [clojure.walk :as w])

  (:import [goog]))

(def search-field-id "airbus-state_viewer_search_field")
(def local-storage-id "airboss-state-viewer")
(def panel-id "airbus-state_viewer_panel")


(defn nt [n t]
  {:name n :type? t})

(def types [(nt "Map" map?)
            (nt "Vector" vector?)
            (nt "Lazy Sequence" (fn [v] (instance? LazySeq v)))
            (nt "Sequence" seq?)
            (nt "nil" nil?)
            (nt "String" string?)
            (nt "Number" number?)
            ;(nt "Big-number" (fn [v] (instance? js/BigNumber v)))
            (nt "Boolean" boolean?)
            (nt "Keyword" keyword?)
            (nt "Symbol" symbol?)
            (nt "Reaction (reagent)" (fn [v] (instance? r-atom/Reaction v)))
            (nt "RAtom (reagent)" (fn [v] (instance? r-atom/RAtom v)))
            (nt "RCursor (reagent)" (fn [v] (instance? r-atom/RCursor v)))
            (nt "Atom" (fn [v] (instance? Atom v)))
            (nt "JavaScript-Object" (fn [v] (instance? js/Object v)))
            (nt "Unknown" (fn [_] true))])

;--------
; styles
;--------

(def postition-styles-panel {:right      (fn [size] {:right 0 :top 0 :height "100%" :width size})
                             :left       (fn [size] {:left 0 :top 0 :height "100%" :width size})
                             :up         (fn [size] {:top 0 :left 0 :width "100%" :height size})
                             :down       (fn [size] {:bottom 0 :left 0 :width "100%" :height size})
                             :fullscreen (fn [_] {:right 0 :top 0 :width "100%" :height "100%"})})


(def style-panel {:z-index        100000
                  :display        :flex
                  :flex-direction :column
                  :position       :absolute
                  :background     "rgba(66, 66, 66, 0.93)"
                  :color          "rgb(201, 201, 201)"
                  ;:border         "2px solid rgb(81, 81, 81)"
                  :box-shadow     "0 5px 5px rgba(101,101,101, 0.5)"})

(def search-field-style {:width         "100%"
                         :color         "rgba(175, 207, 249, 0.7)"
                         :padding-left  6
                         :min-height    25
                         :border        :none
                         :border-bottom "1px solid rgb(81, 81, 81)"
                         :font-size     14
                         :outline       :none
                         :background    "rgba(46, 46, 46, 0.93)"})

(def style-row {:width       "100%"
                :display     :flex
                :align-items :center
                :line-height 1
                :height      25
                :padding     6
                })

(def default-hide-key :ESC)
(def default-show-key :F12)


(def default-setup {:version       3
                    :key-width     200
                    :tab-width     200
                    :open-rows     #{[]}
                    :editing-row   nil
                    :filter-text   ""
                    :position      :fullscreen
                    :size          "45%"
                    :key-code-hide default-hide-key
                    :key-code-show default-show-key})



;---------
; control
;---------

(defn- dom-id-path [p]
  (str (hash (str "state-viewer" p))))

(defn- dom-id-model [m]
  (dom-id-path (:path m)))


(defn- atom? [x]
  (satisfies? IAtom x))

(defn- container? [x]
  (seqable? (if (atom? x) @x x)))

(defn- get-keys [value]
  (cond
    (map? value) (sort-by str (keys value))
    (sequential? value) (sort (mapv identity (range (count value))))
    :else []))

(defn- open? [state-atom path]
  (contains? (:open-rows @state-atom) path))

(defn- editing? [state-atom path]
  (= (:editing-row @state-atom) path))

(declare focus)

(defn- close-row [state-atom path]
  ;dont remove root - should always be visible
  (when-not (= [] path)
    (swap! state-atom update-in [:open-rows] disj path)))

(defn- focus [dom-id]
  (when-let [node (dom-helper/getElement dom-id)]
    (ut/scroll-into-view node (dom-helper/getElement panel-id))
    (.focus node)))

(defn- focus-row [rm]
  (focus (dom-id-model rm)))

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

(defn- contains-string [filter-text s]
  (let [f (re-find (js/RegExp filter-text "i") s)]
    (not (s/blank? f))))

(defn index-of-model
  [models m]
  (first (keep-indexed (fn [i m1] (when (= (:path m) (:path m1)) i)) models)))

(defn- update-position [state-atom position]
  (let [last-p (:position @state-atom)
        new-p (cond
                (and (= last-p :left) (= position :right))
                :fullscreen

                (and (= last-p :right) (= position :left))
                :fullscreen

                (and (= last-p :up) (= position :down))
                :fullscreen

                (and (= last-p :down) (= position :up))
                :fullscreen

                :else position)]
    (swap! state-atom assoc :position new-p)))

(def mk-dispatcher
  (let [handlers-atom (atom nil)
        h-executer (fn [path & args]
                     (if-let [f (get @handlers-atom path)]
                       (apply f args)
                       (do
                         (println path args)
                         (.warn js/console "Unable to dispatch " path " with " args))))]

    (fn [data-atom models state-atom last-focus-atom]
      (reset! handlers-atom {[:app :move-left]    (fn [] (update-position state-atom :left))
                             [:app :move-right]   (fn [] (update-position state-atom :right))
                             [:app :move-up]      (fn [] (update-position state-atom :up))
                             [:app :move-down]    (fn [] (update-position state-atom :down))

                             [:search :on-focus]  (fn [id] (swap! last-focus-atom assoc :last-focus id :rm nil))
                             [:search :on-search] (fn [text] (swap! state-atom assoc-in [:filter-text] text))
                             [:search :move-down] (fn [] (focus-row (first models)))

                             [:row :on-focus]     (fn [rm] (swap! last-focus-atom assoc :last-focus (dom-id-model rm) :rm rm))
                             [:row :start-edit]   (fn [rm] (swap! state-atom assoc :editing-row (:path rm)))
                             [:row :stop-edit]    (fn [rm] (swap! state-atom dissoc :editing-row)
                                                    (focus-row rm))
                             [:row :change-value] (fn [rm v]
                                                    (loop [last-atom data-atom
                                                           path (vec (:path rm))
                                                           index 1]

                                                      (cond
                                                        ;updating at root level in last-atom
                                                        (not (seq path))
                                                        (reset! last-atom v)

                                                        ;updating none root level
                                                        (= index (count path))
                                                        (swap! last-atom assoc-in path v)

                                                        :else
                                                        (do
                                                          (let [value-at-index (get-in @last-atom (subvec path 0 index))]
                                                            (if (atom? value-at-index)
                                                              ;new last-atom
                                                              (recur value-at-index
                                                                     (subvec path index)
                                                                     (inc index))

                                                              ;same last-atom - same path
                                                              (recur last-atom
                                                                     path
                                                                     (inc index)))))))


                                                    (swap! state-atom dissoc :editing-row)
                                                    (focus-row rm))


                             [:row :move-up]      (fn [rm]
                                                    (let [index (index-of-model models rm)]
                                                      (if (= 0 index)
                                                        (focus search-field-id)
                                                        (focus-row (get models (dec index))))))
                             [:row :move-down]    (fn [rm]
                                                    (let [index (index-of-model models rm)]
                                                      (when (> (count models) (dec index))
                                                        (focus-row (get models (inc index))))))
                             [:row :open]         (fn [{:keys [path value]}]
                                                    (let [c (container? value)]
                                                      (when c
                                                        (container? value)
                                                        (swap! state-atom update-in [:open-rows] conj path))))
                             [:row :close]        (fn [{:keys [path value]}]
                                                    (if (and
                                                          (container? value)
                                                          (open? state-atom path))
                                                      (close-row state-atom path)
                                                      (do
                                                        (close-row state-atom (vec (drop-last path)))
                                                        (focus (dom-id-path (vec (drop-last path)))))))})
      h-executer)))


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
  ([value filter-text]
   (let [raw-value (raw-data value)
         paths (reduce (fn [v k] (let [path (build-paths-from-filter k (get raw-value k) filter-text)]
                                   (if path
                                     (into v path) v))) [] (get-keys raw-value))]
     ;also build paths of parents
     (rest (distinct (reduce (fn [v p] (into v (reduce (fn [v1 p1] (conj v1 (into (last v1) [p1]))) [[]] p))) [] paths)))))

  ([k value filter-text]
   (let [raw-value (raw-data value)]
     (if (container? value)
       (reduce (fn [v k1] (let [paths (build-paths-from-filter k1 (get raw-value k1) filter-text)]
                            ;take keys/paths that qualifies by filter-text and put k (key) in front of them
                            (reduce (fn [v1 p] (conj v1 (into [k] p))) v paths))) [] (get-keys raw-value))
       (when (contains-string filter-text (str k))
         [[k]])))))

(defn- build-row-model [edit open path value]
  (let [is-atom (atom? value)]
    {:path      path
     :edit      edit
     :open      open
     :raw-value value
     :value     (if is-atom @value value)
     :is-atom   is-atom}))

(defn- build-row-models [state-atom data]
  (let [filter-text (:filter-text @state-atom)
        paths (if (s/blank? filter-text) (build-paths state-atom data) (build-paths-from-filter data filter-text))]
    (map (fn [p] (build-row-model (editing? state-atom p) (open? state-atom p) p (get-data p data))) paths)))

;--------------
; search-field
;--------------

(defn- search-field [value _]
  (let [state (r/atom {:filter-text value
                       :focus       false})]
    (fn [_ dispatcher]
      [:input {:id        search-field-id
               :style     (merge search-field-style (when (:focus @state)
                                                      {:border-bottom "1px solid rgba(175, 207, 249, 0.7)"}))
               :onChange  (fn [e] (let [v (.-value (.-target e))]
                                    (swap! state assoc :filter-text v)
                                    (when (s/blank? v) (dispatcher [:search :on-search] ""))))
               :value     (:filter-text @state)
               :onFocus   (fn [_]
                            (swap! state assoc :focus true)
                            (dispatcher [:search :on-focus] search-field-id))

               :onBlur    (fn [_] (swap! state assoc :focus false))
               :onKeyDown (fn [e]
                            (when (and (ut/modifier? e) (ut/key? :F e))
                              (.preventDefault e))
                            (when-not (ut/modifier? e)
                              (cond
                                (ut/key? :DOWN e) (do (.preventDefault e) (dispatcher [:search :move-down]))
                                (ut/key? :ENTER e) (do (.preventDefault e) (dispatcher [:search :on-search]
                                                                                       (-> e .-target .-value))))))}])))
;--------------
; render value
;--------------

(defn- render-value-empty-container [{:keys [value]}]
  (cond
    (map? value) "{ }"
    (list? value) "( )"
    (vector? value) "[ ]"
    (set? value) "#{ }"
    (= LazySeq (type value)) "( ) Lazy seq"
    :default "[[[]]]"))

(defn- render-value-container-with-children [{:keys [value is-atom]}]
  (let [s (cond
            (map? value) (str "{+} " (count value))
            (list? value) (str "(+) " (count value))
            (vector? value) (str "[+] " (count value))
            (set? value) (str "#{+} " (count value))
            (= LazySeq (type value)) (str "(+) " (count value) " Lazy seq")
            :default "[[[+]]]")]
    (if is-atom (str "@atom " s) s)))

(defn- render-value-container [{:keys [open value] :as m}]
  (if (seq value)
    (if open
      ""
      (render-value-container-with-children m))
    (render-value-empty-container m)))

(defn- render-value-leaf [{:keys [value edit is-atom] :as m} dispatcher]
  (if edit
    [editor/render value (fn [v] (dispatcher [:row :change-value] m v)) #(dispatcher [:row :stop-edit] m)]
    (if (nil? value)
      ""
      (if is-atom (str "@atom " value) (str value)))))

(defn- render-value [{:keys [value] :as m} dispatcher]
  (if (container? value)
    (render-value-container m)
    (render-value-leaf m dispatcher)))

;------------
; render row
;------------


(defn- render-key [key-value small]
  (cond
    (nil? key-value) "nil"
    (keyword? key-value) (let [key-ns (namespace key-value)
                               key-name (name key-value)]
                           (if key-ns
                             [:div
                              [:span {:style {:color :lightgrey :opacity 0.5}} (if small "::" (str ":" key-ns "/"))] [:span (str key-name)]]
                             [:span (str key-value)]))
    (symbol? key-value) [:span (str "'" key-value)]
    (string? key-value) [:span (str "\"" key-value "\"")]
    :else [:span (str key-value)]))


(defn depth-style [path]
  #_(let [depth (count path)]
      (cond
        (= depth 0) {:background "#F3E5F5"}
        (= depth 1) {:background "#E1BEE7"}
        (= depth 2) {:background "#CE93D8"}
        (= depth 3) {:background "#BA68C8"}
        (= depth 4) {:background "#AB47BC"}
        (= depth 5) {:background "#9C27B0"})))


(defn- render-row [_ _ _]
  (let [focus-state (r/atom false)
        style style-row]
    (fn [{:keys [path] :as model} dispatcher {:keys [key-width tab-width small]}]
      [:div {:id        (dom-id-model model)
             :tab-index 0
             :style     (merge style {:padding-left (+ 4 (* tab-width (- (count path) 1)))}
                               (depth-style path)
                               (when @focus-state {:background "rgba(175, 207, 249, 0.7)"
                                                   :color      :white}))
             :onFocus   (fn [_]
                          (reset! focus-state true)
                          (dispatcher [:row :on-focus] model))

             :onBlur    (fn [_] (reset! focus-state false))

             :onKeyDown (fn [e]
                          (when-not (ut/modifier? e)
                            (cond
                              (ut/key? :ENTER e) (dispatcher [:row :start-edit] model)
                              (ut/key? :LEFT e) (dispatcher [:row :close] model)
                              (ut/key? :RIGHT e) (dispatcher [:row :open] model)
                              (ut/key? :UP e) (do (.preventDefault e) (dispatcher [:row :move-up] model))
                              (ut/key? :DOWN e) (do (.preventDefault e) (dispatcher [:row :move-down] model)))))}

       ;key
       [:div {:style {:min-width key-width :color :white :opacity 1}}
        (render-key (last path) small)]
       ;value
       [:div {:style {:min-width     key-width
                      :white-space   :nowrap
                      :overflow      :hidden
                      :text-overflow :ellipsis}}
        (render-value model dispatcher)]])))


(defn- info-panel [last-focus-atom small]
  (let [{:keys [path raw-value]} (:rm @last-focus-atom)]
    [:div {:style {:background      "rgb(47, 47, 47)"
                   :display         :flex
                   :align-items     :center
                   :justify-content :space-between
                   :padding         6
                   :color           "rgba(175, 207, 249, 0.7)"
                   :min-height      25
                   :max-height      25
                   :width           "100%"}}

     [:div (if path (str (if small (last path) path)) "")]
     [:div (some (fn [{:keys [name type?]}] (when (type? raw-value) name)) types)]]))

(defn- render-rows-panel [row-models dispatcher {:keys [small] :as opts}]
  [:div {:id panel-id :style {:overflow :auto :flex-grow 1}}
   (let [opts (merge opts (when small {:tab-width 15 :key-width 160}))]
     (map (fn [r] ^{:key (dom-id-model r)} [render-row r dispatcher opts]) row-models))])


(defn- data-wash [local-storage]
  (when (and local-storage (= (:version default-setup) (:version local-storage)))
    (let [ls (-> local-storage
                 (update :position keyword)
                 (update :key-code-hide keyword)
                 (update :key-code-show keyword))

          rows (w/postwalk (fn [x]
                             (cond
                               ((some-fn number? vector?) x) x
                               :else (keyword x)))
                           (:open-rows ls))]
      (assoc ls :open-rows (set rows)))))

(defn- prepare-data-for-local-storage [data]
  (let [rows (w/postwalk (fn [x]
                           (if (keyword? x)
                             (let [name-s (namespace x)
                                   n (name x)
                                   s (str (if name-s (str name-s "/" n) n))]
                               s)
                             x))
                         (:open-rows data))]
    (assoc data :open-rows (set rows)
                :editing-row nil)))


(defn- app-key-listeners [dispatcher e]
  (when (ut/alt-modifier? e)
    (cond
      (ut/key? :LEFT e) (do (.preventDefault e) (dispatcher [:app :move-left]))
      (ut/key? :RIGHT e) (do (.preventDefault e) (dispatcher [:app :move-right]))
      (ut/key? :DOWN e) (do (.preventDefault e) (dispatcher [:app :move-down]))
      (ut/key? :UP e) (do (.preventDefault e) (dispatcher [:app :move-up])))))

(defn render
  "Creates a panel that shows the data in the given data-atom. The panel should only be used as a singleton on
  application level - if multiple instances are shown - it will fail"
  [data-atom _]
  (let [local-storage (data-wash (ut/local-storage local-storage-id))
        last-focus-atom (r/atom nil)
        state-atom (r/atom (merge default-setup local-storage))
        row-models (reaction (build-row-models state-atom @data-atom))
        dispatcher (reaction (mk-dispatcher data-atom (vec @row-models) state-atom last-focus-atom))]

    (r/create-class
      {:reagent-render         (fn [_ _]
                                 (let [{:keys [filter-text key-width tab-width size position]} @state-atom
                                       small (contains? #{:left :right} position)]
                                   [:div {:style   (merge style-panel ((get postition-styles-panel position) size))
                                          :onKeyUp (partial app-key-listeners @dispatcher)}

                                    [search-field filter-text @dispatcher]
                                    [render-rows-panel @row-models @dispatcher {:filter-text filter-text
                                                                                :size        size
                                                                                :small       small
                                                                                :position    position
                                                                                :key-width   key-width
                                                                                :tab-width   tab-width}]

                                    [info-panel last-focus-atom small]]))

       :component-did-mount    (fn [_]
                                 (let [last-id (:last-focus @state-atom)
                                       last-node (dom-helper/getElement last-id)
                                       next-id (if last-node last-id search-field-id)]
                                   (focus next-id)))

       :component-will-unmount (fn [_]
                                 (ut/local-storage-write
                                   local-storage-id
                                   (-> @state-atom
                                       (assoc :last-focus (:last-focus @last-focus-atom))
                                       prepare-data-for-local-storage)))})))

(defn- ui-toggler [_ {:keys [key-code-show key-code-hide]}]
  (let [visible (r/atom false)
        k-show (or key-code-show default-show-key)
        k-hide (or key-code-hide default-hide-key)]

    (events/listen js/window "keydown"
                   (fn [e] (cond
                             (ut/key? k-show e) (do (.preventDefault e) (reset! visible (not @visible)))
                             (ut/key? k-hide e) (do (.preventDefault e) (reset! visible false)))))

    (fn [data-atom opts] (when @visible [render data-atom opts]))))

(defn load [data-atom opts]
  (r/render-component [ui-toggler data-atom opts] (-> (.-body js/document)
                                                      (.appendChild (dom-helper/createElement "div")))))
