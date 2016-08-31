(ns bracketbird.util.dom-util
  (:require [goog.dom :as dom-helper]))

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



;--------------
; reagent util
;--------------

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
