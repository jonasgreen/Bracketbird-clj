(ns tools.dom-util)



;-----------------
; Console logging

(defn- js-apply [f target args]
  (.apply f target (to-array args)))

(def log
  (let [types {:log   (.-log js/console)
               :info  (.-info js/console)
               :warn  (.-warn js/console)
               :error (.-error js/console)}]

    (fn [type & args]
      (let [found (get types type)]
        (if found
          (js-apply found js/console args)
          (js-apply (.-warn js/console) js/console
                    (conj args ". You where trying to log:" (str "Logging error. Given type " type " not valid. Valid types are " (keys types)))))))))
