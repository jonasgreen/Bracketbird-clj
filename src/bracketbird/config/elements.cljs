(ns bracketbird.config.elements
  (:require [recontain.core :as rc]
            [restyle.core :as rs]))

(def elements {:icon         {:render   [:i]
                              :decorate [:hover :action]
                              :style    (rs/style :icon)

                              'action   (fn [_] (println "icon 'action please implement... "))}

               :button       {:render    [:div]
                              :decorate  [:hover :active :action]
                              :tab-index 0
                              :style     (fn [{:keys [rc-button-style] :as d}]
                                           (rs/style (if rc-button-style rc-button-style :button)
                                                     {:active? (rc/ls (rc/sub-name d :active?))
                                                      :hover?  (rc/ls (rc/sub-name d :hover?))}))

                              'action    (fn [_] (println "button 'action please implement... "))}

               :large-button {:inherits :button
                              :style    (fn [d]
                                          (rc/super :style (assoc d :rc-button-style :large-button)))}



               :input        {:render      [:input]
                              :decorate    [:hover :change :focus :key-enter-action]
                              :style       {:border :none :padding 0}
                              :type        :text
                              :placeholder "Type some text"

                              'action      (fn [] (println "input 'action please implement "))}})


