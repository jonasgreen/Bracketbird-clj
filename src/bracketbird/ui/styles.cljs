(ns bracketbird.ui.styles)


(def font-color "rgba(55, 71, 79, 1)")

(def menu-font-size 22)
(def menu-height 62)

(def menu-side-space 75)
(def page-top-space 50)
(def page-side-space 100)


(def menu-panel-style {:display       :flex
                       :align-items   :center
                       :height        menu-height
                       :padding-left  menu-side-space
                       :padding-right menu-side-space})

(def menu-item-style {:font-size      menu-font-size
                      :margin-right   40
                      :letter-spacing 1.2
                      :opacity 0.5
                      :cursor         :pointer})

(def page-style {:height "100%"
                 :overflow-y :scroll :padding-left page-side-space :padding-right page-side-space :padding-top page-top-space})
