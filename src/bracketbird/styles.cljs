(ns bracketbird.styles)


(def row-height 30)

(def font-color "rgba(55, 71, 79, 1)")

(def menu-font-size 22)
(def menu-height 62)

(def menu-side-space 80)
(def page-top-space 40)
(def page-bottom-space 40)
(def page-side-space 120)

(def input-text-field {:border  :none
                       :padding 0})

(def menu-panel-style {:display       :flex
                       :align-items   :center
                       :min-height    menu-height
                       :padding-top   4
                       :padding-left  menu-side-space
                       :padding-right menu-side-space})

(def menu-item-style {:font-size      menu-font-size
                      :margin-right   40
                      :letter-spacing 1.2
                      :opacity        0.5
                      :cursor         :pointer})

(def tournament-page-style {:height         "100vh"
                            :display        :flex
                            :flex-direction :column})

(def tournament-tab-content-style {:padding-bottom page-bottom-space
                                  :border-top      "1px solid rgba(255,255,255,0)"
                                  :height          :100%
                                  :overflow-y      :auto
                                  :padding-left    page-side-space
                                  :padding-right   page-side-space
                                  :padding-top     page-top-space})
