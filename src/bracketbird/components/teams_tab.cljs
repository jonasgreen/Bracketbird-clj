(ns bracketbird.components.teams-tab
  (:require [reagent.core :as r]
            [recontain.core :as rc]
            [bracketbird.styles :as s]
            [bracketbird.dom :as d]
            [bracketbird.util :as ut]
            [bracketbird.rc-util :as rc-util]))


(defn enter-team-input [handle {:keys [value]} _]
  [:div {:style {:display     :flex
                 :margin-top  30
                 :align-items :center}}

   [:input (merge {:id          (rc/id handle "input")
                   :placeholder "Enter team"
                   :type        :text
                   :style       s/input-text-field
                   :value       value}
                  (rc-util/input-handlers handle))]

   [:button {:class    "primaryButton"
             :on-click #(rc/dispatch handle :create-team)}
    "Add Team"]])


(defn team-row [handle {:keys [value]} {:keys [hook/team]} index]
  [:div {:style {:display :flex :align-items :center :min-height 30}}
   [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc index)]
   [:input (merge {:id    (rc/id handle "team-name")
                   :style (merge s/input-text-field {:min-width 200})
                   :value (if value value (:team-name team))}
                  (rc-util/input-handlers handle))]])


(defn render [handle local-state {:keys [hook/teams-order hook/teams]}]
  (let [{:keys [scroll-top
                scroll-height
                client-height]} local-state]
    [:div {:style    (merge
                       {:display        :flex
                        :flex-direction :column
                        :height         :100%}
                       (when (< 0 scroll-top) {:border-top "1px solid rgba(241,241,241,1)"}))
           :on-click (fn [e] ())}

     ; teams table
     [:div {:id        (rc/id handle "scroll")
            :style     (merge {:padding-top    40
                               :padding-left   120
                               :max-height     :100%
                               :min-height     :200px
                               :padding-bottom 40
                               :overflow-y     :auto}
                              (when (not= (+ scroll-top client-height)
                                          scroll-height) {:border-bottom "1px solid rgba(241,241,241,1)"}))
            :on-scroll (fn [e] (->> e .-target ut/scroll-data (rc/put! handle merge)))}

      (map (fn [team-id index]
             ^{:key team-id} [rc/build handle {:team-id team-id} team-row index]) teams-order (range (count teams)))]

     ; input field
     [:div {:style {:padding-left 120 :padding-bottom 20}}
      [rc/build handle {} enter-team-input]]]))