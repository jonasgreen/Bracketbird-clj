(ns bracketbird.components.teams-tab
  (:require [recontain.core :as rc]
            [bracketbird.style :as s]
            [bracketbird.util :as ut]
            [restyle.core :as rs]))


(defn enter-team-input [h {:keys [input-value] :as _} _]
  [:div {:style (rs/style :enter-team-input)}
   [rc/ui :input {:id          :input
                  :placeholder "Enter team"
                  :type        :text
                  :style       s/input-text-field
                  :value       input-value
                  :events      [:key :change]}]

   [rc/ui :button {:id     :button
                   :class  "primaryButton"
                   :events [:key :click]} "Add Team"]])

(defn team-row [h {:keys [top-hover? delete-icon-hover? input-value]} {:keys [hook/team]} index]
  [rc/ui :div {:id     :top
               :style  (rs/style :teams-row)
               :events [:hover]}

   [rc/ui :div {:id     :delete-icon
                :style  (rs/style :teams-row-icons {:icon-hover? delete-icon-hover? :row-hover? top-hover?})
                :events [:hover :click] }
    [ut/icon {:style (rs/style :teams-row-delete-icon {:icon-hover? delete-icon-hover? :row-hover? top-hover?})} "clear"]]

   [:div {:style (rs/style {:width [:page-padding]})}]
   [:div {:style {:display :flex :align-items :center :width 30 :opacity 0.5 :font-size 10}} (inc index)]
   [rc/ui :input {:id     :input
                  :style  (merge s/input-text-field {:min-width 200})
                  :value  (if input-value input-value (:team-name team))
                  :events [:change :key :focus]}]])


(defn render [h {:keys [table-scroll-top table-scroll-bottom]} {:keys [hook/teams-order hook/teams]}]
  [:div {:style (rs/style :tab-content {:scroll-top table-scroll-top})}

   ; teams table
   [rc/ui :div {:id     :table
                :style  (rs/style :teams-table {:scroll-bottom table-scroll-bottom})
                :events [:scroll]}

    (map (fn [team-id index]
           ^{:key team-id} [rc/container {:team-id team-id} team-row index]) teams-order (range (count teams)))]

   ; input field
   [rc/container {} enter-team-input]])