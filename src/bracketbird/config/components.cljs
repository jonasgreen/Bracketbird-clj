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


(def components {:button       {:render      (fn [{:keys [text]}] [:div (or text "a button")])
                                :decorate    [:hover :active]
                                :style       (fn [{:keys [rc-button-style] :as d}]
                                               (rs/style (if rc-button-style rc-button-style :button)
                                                         {:active? (rc/ls (rc/sub-name d :active?))
                                                          :hover?  (rc/ls (rc/sub-name d :hover?))}))
                                :on-click    #(rc/call 'action)
                                :on-key-down (fn [{:keys [rc-event]}]
                                               (d/handle-key
                                                 rc-event
                                                 {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                                'action      (fn [_] (println "button 'action ... "))}

                 :large-button {:inherits :button
                                :style    #(rc/super :style (assoc % :rc-button-style :large-button))}


                 :input        {:render      (fn [_] [:input])
                                :decorate    [:hover :change :focus]
                                :style       {:border :none :padding 0}
                                :type        :text
                                :placeholder "Type som text"
                                :on-key-down (fn [{:keys [rc-event]}]
                                               (d/handle-key
                                                 rc-event
                                                 {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))

                                'focus       (fn [] (rc/focus-dom-element :input))
                                'action      (fn [] (println "input 'action ... " (rc/ls :input-value)))}})





