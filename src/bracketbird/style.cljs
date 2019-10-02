(ns bracketbird.style
  (:require [restyle.core :as rs]))


(def styles {
             ; colors
             :border-color          (rs/rgba 241, 241, 241, 1)

             ;borders
             :border                [rs/border 1 "solid" :border-color]

             ; layout
             :layout-unit           40

             :app-padding           [* 2 :layout-unit]

             :page-padding          [:layout-unit]

             :page-menu-space       [:layout-unit]


             :row-height            30


             :tournament-page       {:height         "100vh"
                                     :display        :flex
                                     :flex-direction :column}

             :tab-content           (fn [{:keys [scroll-top]}]
                                      (merge {:display        :flex
                                              :flex-direction :column
                                              :height         :100%}
                                             (when (< 0 scroll-top) {:border-top [:border]})))

             :teams-table           (fn [{:keys [scroll-bottom]}]
                                      (merge {:padding-top    [:layout-unit]
                                              :max-height     :100%
                                              :min-height     :200px
                                              :padding-bottom [:layout-unit]
                                              :overflow-y     :auto}
                                             (when (< 0 scroll-bottom) {:border-bottom [:border]})))

             :teams-row             {:display :flex :align-items :center :min-height [:row-height]}

             :teams-row-icons       (fn [{:keys [icon-hover? row-hover?]}] {:display         :flex
                                                            :align-items     :center
                                                            :height          [:row-height]
                                                            :justify-content :center
                                                            :cursor          (if icon-hover? :pointer :normal)
                                                            :width           [:app-padding]})


             :teams-row-delete-icon (fn [{:keys [icon-hover? row-hover?]}]
                                      (merge {:font-size 8 :opacity 0.5 :transition "background 0.2s, color 0.2s, border-radius 0.2s"}
                                             (when-not row-hover?
                                               {:color :transparent})

                                             (when icon-hover?
                                               {:font-weight   :bold
                                                :background    :red
                                                :color         :white
                                                :font-size 10
                                                :border-radius 8})))


             :teams-row-seeding     {:display :flex :align-items :center :width [:row-height] :opacity 0.5 :font-size 10}

             :enter-team-input      {:padding-left [+ :app-padding :page-padding]
                                     :display      :flex
                                     :min-height   [:app-padding]
                                     :align-items  :center}
             })



(def menu-font-size 22)

(def page-top-space 40)
(def page-bottom-space 40)
(def page-side-space 120)

(def input-text-field {:border  :none
                       :padding 0})


(def menu-panel-style {:font-size      menu-font-size
                       :display        :flex
                       :align-items    :center
                       :min-height     [:app-padding]
                       :padding-left   [:app-padding]
                       :letter-spacing 1.2
                       :padding-right  [:app-padding]})

(def menu-item-style {:margin-right [:layout-unit]
                      :opacity      0.5
                      :cursor       :pointer})

(def tournament-page-style {:height         "100vh"
                            :display        :flex
                            :flex-direction :column})

(def tournament-tab-content-style {:padding-bottom page-bottom-space
                                   :border-top     "1px solid rgba(255,255,255,0)"
                                   :height         :100%
                                   :overflow-y     :auto
                                   :padding-left   page-side-space
                                   :padding-right  page-side-space
                                   :padding-top    page-top-space})
