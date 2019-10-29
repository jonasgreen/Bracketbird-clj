(ns bracketbird.config.components
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


(def components {:primary-button {:render              (fn [_] [::button "a button"])

                                  [:button :style]     (fn [_]
                                                         (println "xecuting basic style")
                                                         (rs/style :primary-button {:button-active? (rc/ls :button-active?)
                                                                                    :button-hover?  (rc/ls :button-hover?)}))
                                  ;[:button :event->state] (fn [_] [:hover :click])

                                  [:button :on-click]  (fn [_] (println "primary button click"))
                                  [:button :on-key-up] (fn [_])

                                  }})


