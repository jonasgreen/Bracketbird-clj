(ns bracketbird.config.ui-elements
  (:require [recontain.core :as rc]
            [restyle.core :as rs]))

; configuration of elements:
; :render
; :options - items also present in config, will be overwritten by config.
; :style
; :events
; :on-click, :on-key-down :on-change etc (classic event setup)
; :pass-in
;
; When items are overwritten, parent items will be present as 'render 'style etc.
;
;
; Merge direction: (merge config-from-item-options item-config config-from-parent-options parents-config)



(def elements {:primary-button {:render                (fn [state]
                                                         [::button {:events [:click :hover? :key]} "a button"])

                                [:button :style]       (fn [state]
                                                         (rs/style :primary-button {:button-active? (rc/ls :button-active?)
                                                                                    :button-hover?  (rc/ls :button-hover?)}))

                                [:button :on-click]    (fn [state e] (println "primary button click"))}})


