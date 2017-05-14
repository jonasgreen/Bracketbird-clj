(ns airboss.state-editor
  (:require [reagent.core :as r]
            [cljs.reader :as reader]
            [goog.date :as date]
            [airboss.utils :as ut]
            [goog.dom :as dom-helper])

  (:import [goog]))


(def input-field-id "airboss.stateviewer.editor-input")

(defrecord Edit-mode [name type? parser-fn])

(def edit-modes [(Edit-mode. "string" string? str)
                 (Edit-mode. "number" number? reader/read-string)
                 ;(Edit-mode. "big-number" (fn [v] (instance? js/BigNumber v)) (fn [v] (js/BigNumber. v)))
                 (Edit-mode. "boolean" boolean? reader/read-string)
                 (Edit-mode. ":keyword" keyword? keyword)
                 (Edit-mode. "symbol" symbol? symbol)

                 ;best guess should always be last - kind of fallback.
                 (Edit-mode. "edn" (fn [v] true) reader/read-string)])

;---------
; Parsing
;---------

(defn throw-exception [s expected-type]
  (throw (js/Error. (str "Unable to create a " expected-type " from \"" s "\""))))

(defn- parse-and-validate [s {:keys [name type? parser-fn]}]
  (try
    (let [v (parser-fn s)]
      (if (type? v)
        v
        (throw-exception s name)))
    (catch :default e (throw-exception s name))))

(defn- get-editing-mode [value]
  (if (nil? value) (last edit-modes)
                   (some (fn [v] (when ((:type? v) value) v)) edit-modes)))

(defn- error-field [error]
  (let [{:keys [left top width]} (ut/absolut-position (dom-helper/getElement input-field-id))]
    [:div {:style {:z-index       5000000
                   :position      :fixed
                   :background    "#FF5722"
                   :overflow-x    :hidden
                   :color         :white
                   :top           (+ top 23)
                   :left          left
                   :line-height   1.4
                   :box-shadow    "0 5px 5px rgba(71,71,71, 1)"
                   :padding       4
                   :text-overflow :ellipsis
                   :width         width
                   :white-space   :normal
                   }} error]))

(defn render-input-field [value on-value-changed on-cancel]
  (let [state-atom (r/atom {:value     (str value)
                            :edit-mode (get-editing-mode value)
                            :error     nil})]
    (fn [_ _ _]
      (let [{:keys [value error] :as state} @state-atom
            {:keys [name] :as edit-mode} (:edit-mode state)]
        [:div {:style {:display :flex :align-items :center :position :relative}}
         [:input {:id          input-field-id
                  :type        :text
                  :on-change   (fn [e] (swap! state-atom assoc :value (-> e .-target .-value)))
                  :on-key-down (fn [e]
                                 (when error (swap! state-atom assoc :error nil))
                                 (.stopPropagation e)
                                 (when-not (ut/modifier? e)
                                   (cond
                                     (ut/key? :DOWN e)
                                     (do (.preventDefault e)
                                         (swap! state-atom assoc :edit-mode (ut/cyclic-next edit-mode edit-modes)))

                                     (ut/key? :UP e)
                                     (do (.preventDefault e)
                                         (swap! state-atom assoc :edit-mode (ut/cyclic-previous edit-mode edit-modes)))

                                     (ut/key? :ENTER e)
                                     (try
                                       (->> (parse-and-validate value edit-mode)
                                            (on-value-changed))
                                       (catch js/Error e (swap! state-atom assoc :error (.-message e))))

                                     (ut/key? :ESC e) (on-cancel))))
                  :value       value
                  :style       {:color        "rgba(175, 207, 249, 0.7)"
                                :padding-left 4
                                :border       (str "1px solid " (if error "#FF5722" "transparent"))
                                :min-width    160
                                :min-height   23
                                :font-size    13
                                :outline      :none
                                :background   "rgba(46, 46, 46, 0.93)"}}]
         [:div {:style {:display      :flex
                        :align-items  :center
                        :height       23
                        :padding-left 6
                        :text-align   :center
                        :color        :white}} name]

         (when error
           [error-field error])]))))


(defn render [_ _ _]
  (r/create-class
    {:reagent-render      (fn [value on-value-changed on-cancel]
                            [render-input-field value on-value-changed on-cancel])
     :component-did-mount (fn [this]
                            (when-let [node (dom-helper/getElement input-field-id)]
                              (.focus node)))}))



