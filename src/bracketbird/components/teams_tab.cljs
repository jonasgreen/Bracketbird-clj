(ns bracketbird.components.teams-tab
  (:require [recontain.core :as rc]
            [bracketbird.styles :as s]
            [bracketbird.util :as ut]
            [bracketbird.rc-util :as rc-util]))


(defn enter-team-input [h {:keys [input-value]} _]
  [:div {:style {:display :flex :margin-top 30 :align-items :center}}
   [:input (-> {:id          (rc/id h "input")
                :placeholder "Enter team"
                :type        :text
                :style       s/input-text-field
                :value       input-value}
               (rc-util/bind-events h [:KEY :CHANGE]))]

   [:button (-> {:id (rc/id h "button") :class "primaryButton"}
                (rc-util/bind-events h [:KEY :CLICK]))
    "Add Team"]])


(defn team-row [h {:keys [top-hover? delete-icon-hover? input-value]} {:keys [hook/team]} index]
  [:div (-> {:id    (rc/id h "top")
             :style {:display :flex :align-items :center :min-height 30} }
            (rc-util/bind-events h :HOVER))
   [:div (-> {:id    (rc/id h "delete-icon")
              :style {:display         :flex
                      :align-items     :center
                      :height          20
                      :justify-content :center
                      :cursor (if delete-icon-hover? :pointer :normal)
                      :width           80}}
             (rc-util/bind-events h [:HOVER :CLICK]))
    (when top-hover?
      [ut/icon {:style (merge {:font-size 8 :opacity 0.5} (when delete-icon-hover? {:font-weight :bold
                                                                                    :background :red :color :white :border-radius 8} ))} "clear"])]
   [:div {:style {:width 40}}]
   [:div {:style {:display :flex :align-items :center :width 30 :opacity 0.5 :font-size 10}} (inc index)]
   [:input (-> {:id    (rc/id h "input")
                :style (merge s/input-text-field {:min-width 200})
                :value (if input-value input-value (:team-name team))}
               (rc-util/bind-events h [:CHANGE :KEY :FOCUS]))]])


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