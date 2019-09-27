(ns bracketbird.components.teams-tab
  (:require [recontain.core :as rc]
            [bracketbird.styles :as s]
            [bracketbird.util :as ut]))


(defn enter-team-input [h {:keys [input-value] :as _} _]
  [:div {:style {:display :flex :margin-top 30 :align-items :center}}
   [rc/ui :input {:id          "input"
                  :placeholder "Enter team"
                  :type        :text
                  :style       s/input-text-field
                  :value       input-value
                  :events      [:key :change]}]

   [rc/ui :button {:id     "button"
                   :class  "primaryButton"
                   :events [:key :click]} "Add Team"]])


(defn team-row [h {:keys [top-hover? delete-icon-hover? input-value]} {:keys [hook/team]} index]
  [rc/ui :div {:id     "top"
               :style  {:display :flex :align-items :center :min-height 30}
               :events [:hover]}

   [rc/ui :div {:id     "delete-icon"
                :style  {:display         :flex
                         :align-items     :center
                         :height          20
                         :justify-content :center
                         :cursor          (if delete-icon-hover? :pointer :normal)
                         :width           80}
                :events [:hover :click]}
    (when top-hover?
      [ut/icon {:style (merge
                         {:font-size 8 :opacity 0.5}
                         (when delete-icon-hover?
                           {:font-weight :bold
                            :background  :red :color :white :border-radius 8}))} "clear"])]

   [:div {:style {:width 40}}]
   [:div {:style {:display :flex :align-items :center :width 30 :opacity 0.5 :font-size 10}} (inc index)]
   [rc/ui :input {:id     "input"
                  :style  (merge s/input-text-field {:min-width 200})
                  :value  (if input-value input-value (:team-name team))
                  :events [:change :key :focus]}]])


(defn render [h {:keys [table-scroll-top table-scroll-bottom]} {:keys [hook/teams-order hook/teams]}]
  [:div {:style (merge
                  {:display        :flex
                   :flex-direction :column
                   :height         :100%}
                  (when (< 0 table-scroll-top) {:border-top "1px solid rgba(241,241,241,1)"}))}

   ; teams table
   [rc/ui :div {:id     "table"
                :style  (merge {:padding-top    40
                                :max-height     :100%
                                :min-height     :200px
                                :padding-bottom 40
                                :overflow-y     :auto}
                               (when (< 0 table-scroll-bottom) {:border-bottom "1px solid rgba(241,241,241,1)"}))
                :events [:scroll]}

    (map (fn [team-id index]
           ^{:key team-id} [rc/container {:team-id team-id} team-row index]) teams-order (range (count teams)))]

   ; input field
   [:div {:style {:padding-left 120 :padding-bottom 20}}
    [rc/container {} enter-team-input]]])