(ns bracketbird.style
  (:require [restyle.core :as rs]))


(def styles {
             ; colors
             :border-color      (rs/rgba 241, 241, 241, 1)

             ;borders
             :border            [rs/border 1 "solid" :border-color]

             ; layout
             :layout-unit       40

             :seeding-width     30

             :app-padding       [* 2 :layout-unit]

             :page-padding      [:layout-unit]

             :page-menu-space   [:layout-unit]

             :stage-width       200

             :stage-line        {:width      2
                                 :height     20
                                 :background :red}

             :stage-hook-end    {:width         8
                                 :height        4
                                 :border-radius "4px 4px 0 0"
                                 :background    :red}


             :row-height        30


             :tournament-page   {:height         "100vh"
                                 :display        :flex
                                 :flex-direction :column}

             :tab-content       (fn [{:keys [scroll-top]}]
                                  (merge {:display        :flex
                                          :flex-direction :column
                                          :height         :100%}
                                         (when (< 0 scroll-top) {:border-top [:border]})))

             :teams-table       (fn [{:keys [scroll-bottom]}]
                                  (merge {:padding-top    [:layout-unit]
                                          :max-height     :100%
                                          :min-height     :200px
                                          :padding-bottom [:layout-unit]
                                          :overflow-y     :auto}
                                         (when (< 0 scroll-bottom) {:border-bottom [:border]})))

             :teams-row         {:display :flex :align-items :center :min-height [:row-height]}

             :teams-row-icons   (fn [{:keys [icon-hover? row-hover?]}] {:display         :flex
                                                                        :align-items     :center
                                                                        :height          [:row-height]
                                                                        :justify-content :center
                                                                        :cursor          (if icon-hover? :pointer :normal)
                                                                        :width           [:app-padding]})


             :button            (fn [{:keys [active? hover?]}]
                                  (merge {
                                          :box-shadow     "0 1px 1px rgba(0, 0, 0, 0.2)"
                                          :background     "#3B9EBF"
                                          :color          :white
                                          :display        :inline-block
                                          :font-size      16
                                          :padding-top    5
                                          :padding-left   15
                                          :padding-right  15
                                          :padding-bottom 3
                                          :font-family    "klavika"
                                          :border         "1px solid #0097BF"
                                          :border-radius  2
                                          :white-space    :nowrap
                                          :z-index        20}
                                         (when hover?
                                           {:cursor     :pointer
                                            :box-shadow "0 2px 3px rgba(0, 0, 0, 0.3)"})

                                         (when active?
                                           {:box-shadow "0 1px 1px rgba(0, 0, 0, 0.2)"})))

             :large-button      (fn [{:keys [active? hover?] :as params}]
                                  {:inherit        [:button params]
                                   :font-size      24
                                   :padding-top    10
                                   :padding-left   16
                                   :padding-right  16
                                   :padding-bottom 6})


             :delete-icon       (fn [{:keys [hover?]}]
                                  (merge {:font-size  8
                                          :opacity    0.5
                                          :transition "background 0.2s, color 0.2s, border-radius 0.2s"}
                                         (when hover?
                                           {:font-weight   :bold
                                            :background    :red
                                            :color         :white
                                            :font-size     10
                                            :border-radius 8})))


             :teams-row-seeding {:display :flex :align-items :center :width [:row-height] :opacity 0.5 :font-size 10}

             :enter-team-input  {:padding-left [+ :app-padding :page-padding]
                                 :display      :flex
                                 :min-height   [:app-padding]
                                 :align-items  :center}})

