(ns restyle.core
  (:require [tools.dom-util :as dom-util]))

(defonce ^:private style-definitions (atom {}))
(defonce ^:private color-definitions (atom {}))


(defn- color-to-str [{:keys [red green blue opacity]}]
  (str "rgba(" red "," green "," blue "," opacity ")"))

(defn rgba [r g b opacity] {:red r :green g :blue b :opacity opacity})


(def styles-map {:left            10
                 :right           10
                 :transition-time 2

                 :width           [+ :left :right]

                 :box             (fn [{:keys [padding]}]
                                    {:with    [:width]
                                     :padding padding})

                 :large-box       (fn [{:keys [padding]}]
                                    {:display    :flex
                                     :transition [str "width " :transition-time "s"]
                                     :padding    (if padding padding 10)
                                     :width      [:width]})})


(defn resolve-style [styles value params]
  (cond (keyword? value)
        (do
          (when-let [fv (get styles value)]
            (resolve-style styles fv params)))

        (vector? value)
        (do
          (if (fn? (first value))
            ;function
            (let [children (vec (rest value))
                  ps (->> children (mapv (fn [c] (resolve-style styles c nil))))]
              (apply (first value) ps))
            ;keyword
            (resolve-style styles (first value) nil)))


        ;anonymous functions in styles must return maps
        (fn? value)
        (do
          (let [[p & kvs :as opts] params
                ;; TODO validate p and kvs
                additional-styling (apply hash-map (if (map? p) kvs opts))
                temp-result (if (map? p) (value p) (value))]

            (-> (resolve-style styles temp-result nil)
                (merge additional-styling))))

        (map? value)
        (do
          (let [inheritance (:inherit value)]
            (->> (dissoc value :inherit)
                 (reduce-kv (fn [m k v]
                              (assoc m k (if (vector? v) (resolve-style styles v nil) v)))
                            {}))))

        :else value)
  )

(defn style [k & params]
  (resolve-style styles-map k params)
  ;(resolve-values) -- colors data etc
  )


;; input 1) vector, key,
;;
;; case vector - (first element: function or key - rest is parameters, keys or vectors)


;-----------------------
; Google material colors

(def blue-grey-50 (rgba 236 239 241 1))
(def blue-grey-100 (rgba 206 216 220 1))
(def blue-grey-500 (rgba 96 125 139 1))

(defn register-styles [style-map]
  (swap! style-definitions merge style-map))

(defn register-colors [color-map]
  (swap! color-definitions merge color-map))

;------------------------------
; Color manipulation functions

#_(defn cf
    "Color function.
    Finalises color as a CSS string.
    Takes a color and optionally one or more color transformation functions
    and returns a string usable in CSS"
    ([k] (cf k identity))
    ([k fn & fns]
     (let [c (get @color-definitions k)]
       (if c
         (let [fs (-> (conj fns fn) reverse)
               comp_fn (apply comp fs)]
           (color-to-str (comp_fn c)))

         (do (dom-util/log :warn (str "Color " k " not found."))
             "")))))
