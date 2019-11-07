(ns bracketbird.config.components
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.dom :as d]))

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

(def components {:primary-button {[:render]              (fn [{:keys [text]}]
                                                           [::button {:decorate [:hover :active]} (or text "a primary button")])

                                  [:button :style]       (fn [_] (rs/style :primary-button {:active? (rc/ls :button-active?)
                                                                                            :hover?  (rc/ls :button-hover?)}))
                                  [:button :on-click]    (fn [_] (rc/call 'action))

                                  [:button :on-key-down] (fn [{:keys [rc-event]}]
                                                           (d/handle-key rc-event {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                                  'action                (fn [_] (println "default-button 'action ... "))}


                 :default-input  {[:render]             (fn [_] [::input {:element  :input
                                                                          :decorate [:hover :change :focus]}])

                                  [:input :type]        :text
                                  [:input :placeholder] "Type som text"
                                  [:input :style]       (fn [_] {:border :none :padding 0})

                                  [:input :on-key-down] (fn [{:keys [rc-event]}]
                                                          (d/handle-key rc-event {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                                  'action               (fn [_] (println "default-input 'action ... " (rc/ls :input-value)))}
                 }

  )





