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

(def components {:primary-button {[:render] (fn [{:keys [text]}]
                                              [::button (or text "a primary button")])

                                  [:button] {:decorate    [:hover :active]
                                             :style       #(rs/style :primary-button {:active? (rc/ls :button-active?)
                                                                                      :hover?  (rc/ls :button-hover?)})
                                             :on-click    #(rc/call 'action)
                                             :on-key-down (fn [{:keys [rc-event]}]
                                                            (d/handle-key
                                                              rc-event
                                                              {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))}

                                  'action   (fn [_] (println "default-button 'action ... "))}


                 :default-input  {[:render] (fn [_] [::input :input])

                                  [:input]  {:decorate    [:hover :change :focus]
                                             :style       {:border :none :padding 0}
                                             :type        :text
                                             :placeholder "Type som text"
                                             :on-key-down (fn [{:keys [rc-event]}]
                                                            (d/handle-key
                                                              rc-event
                                                              {[:ENTER] (fn [_] (rc/call 'action) [:STOP-PROPAGATION :PREVENT-DEFAULT])}))}

                                  'focus    (fn [] (rc/focus-dom-element :input))
                                  'action   (fn [] (println "default-input 'action ... " (rc/ls :input-value)))}
                 }

  )





