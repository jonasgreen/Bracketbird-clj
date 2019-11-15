(ns bracketbird.config.components
  (:require [recontain.core :as rc]
            [restyle.core :as rs]
            [bracketbird.dom :as d]))



(def elements {:icon         {:render [:i]
                              :style  {:font-family             "Material Icons"
                                       :font-weight             "normal"
                                       :font-style              "normal"
                                       :font-size               8
                                       :display                 "inline-block"
                                       :width                   "1em"
                                       :height                  "1em"
                                       :opacity                 0.5
                                       :line-height             "1"
                                       :text-transform          "none"
                                       :letter-spacing          "normal"
                                       :word-wrap               "normal"
                                       ;Support for all WebKit browsers.
                                       :-webkit-font-smoothing  "antialiased"
                                       ;Support for Safari and Chrome.
                                       :text-rendering          "optimizeLegibility"
                                       ;Support for Firefox.
                                       :-moz-osx-font-smoothing "grayscale"
                                       ;Support for IE.
                                       :font-feature-settings   "liga"
                                       :transition "background 0.2s, color 0.2s, border-radius 0.2s"}}

               :button       {:render      [:div]
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


               :input        {:render      [:input]
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








#_(def components {:primary-button {[:render] (fn [{:keys [text]}]
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
                   })




