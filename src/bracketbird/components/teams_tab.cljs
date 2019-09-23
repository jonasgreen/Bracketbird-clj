(ns bracketbird.components.teams-tab
  (:require [reagent.core :as r]
            [recontain.core :as rc]
            [bracketbird.styles :as s]
            [bracketbird.dom :as d]
            [bracketbird.util :as ut]))


#_(defn move-entity-focus-up [entities entity sub-key]
    (-> (e/previous-entity entities entity)
        (ut/focus-by-entity sub-key)))

#_(defn move-entity-focus-down [entities entity sub-key]
    (-> (e/next-entity entities entity)
        (ut/focus-by-entity sub-key)))

#_(defn component-dispatcher [ctx enter-team-ctx]
    (let [config {:team-name  {:up     (fn [t] (move-entity-focus-up () #_(t-ctrl/teams ctx) t :team-name))
                               :down   (fn [t] (when-not (move-entity-focus-down () #_(t-ctrl/teams ctx) t :team-name)
                                                 (ut/focus-by-ui-ctx enter-team-ctx :enter-team)))

                               :left   (fn [t] (ut/focus-by-entity t :team-id))
                               :update (fn [t team-name])
                               :inject (fn [t] () #_(let [index (t-ctrl/index-of-team ctx t)]
                                                      (t-ctrl/insert-team ctx "" index)))

                               :delete (fn [t] (let [next-focus-team (or () #_(t-ctrl/next-team ctx t) () #_(t-ctrl/previous-team ctx t))]
                                                 () #_(t-ctrl/delete-team ctx t)
                                                 (if next-focus-team
                                                   (ut/focus-by-entity next-focus-team :team-name)
                                                   (ut/focus-by-ui-ctx enter-team-ctx :enter-team))))}


                  :enter-team {:up          (fn []
                                              (when-let [last-team () #_(t-ctrl/last-team ctx)]
                                                (ut/focus-by-entity last-team :team-name)))

                               :create-team (fn [team-name]
                                              () #_(t-ctrl/add-team ctx team-name)
                                              #_(old-context/update-ui! enter-team-ctx ""))}}]

      (fn [path & args]
        (if-let [f (get-in config path)]
          (apply f args)
          (.warn js/console "Unable to dispatch " path " with " args)))))



(defn enter-team-input [handle {:keys [team-name]} _]
  [:div {:style {:display     :flex
                 :margin-top  30
                 :align-items :center}}
   [:input {:id          (rc/id handle "input")
            :placeholder "Enter team"
            :type        :text
            :style       s/input-text-field
            :value       team-name
            :on-key-down (d/key-handler {[:ENTER] (fn [e] (rc/dispatch handle :create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                         [:UP]    (fn [e]
                                                    (-> (:ctx handle)
                                                        (rc/get-handle :hook/ui-teams-tab)
                                                        (rc/dispatch :focus-last-team)))})

            :on-key-up   (d/key-handler {[:BACKSPACE] (fn [e] (println "key-up" team-name))})

            :on-change   #(->> % ut/value (rc/put! handle assoc :team-name))}]

   [:button {:class    "primaryButton"
             :on-click #(rc/dispatch handle :create-team)
             } "Add Team"]])


(defn team-row_old [position ctx]
  (let [team nil #_(ui/hook :hook/team ctx)
        team-name-state (r/atom (:team-name team))
        delete-by-backspace (atom (clojure.string/blank? (:team-name team)))]
    (fn [position _]

      [:div {:style {:display     :flex
                     :align-items :center
                     :min-height  30}}
       [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc position)]
       [:input {:id          nil
                :style       (merge s/input-text-field {:min-width 200})
                :value       @team-name-state


                #_(:on-key-down {:ENTER     {d/no-modifiers?   #(dispatcher [:team-name :down] team)
                                             d/shift-modifier? #(dispatcher [:team-name :inject] team)}
                                 :BACKSPACE {#(@delete-by-backspace) (dispatcher [:team-name :delete] team)}

                                 :DOWN      {d/no-modifiers? #(dispatcher [:team-name :down] team)}
                                 :UP        {d/no-modifiers? #(dispatcher [:team-name :up] team)}
                                 :ELSE      {#(not (clojure.string/blank? @team-name-state)) (reset! delete-by-backspace false)}})



                :on-key-down (fn [e]
                               (cond (and (d/key? :BACKSPACE e) @delete-by-backspace)
                                     #_(dispatcher [:team-name :delete] team)

                                     (d/key? #{:ENTER :SHIFT} e)
                                     (do
                                       (.stopPropagation e)
                                       (.preventDefault e)
                                       #_(dispatcher [:team-name :down] team))

                                     (d/key-and-modifier? :ENTER d/shift-modifier? e)
                                     (do
                                       (.stopPropagation e)
                                       (.preventDefault e)
                                       #_(dispatcher [:team-name :inject] team))

                                     (d/key? :DOWN e)
                                     (do (.stopPropagation e)
                                         (.preventDefault e)
                                         #_(dispatcher [:team-name :down] team))

                                     (d/key? :UP e)
                                     #_(dispatcher [:team-name :up] team)

                                     :else (when-not (clojure.string/blank? @team-name-state)
                                             (reset! delete-by-backspace false))))

                :on-key-up   (fn [e] (when (clojure.string/blank? @team-name-state)
                                       (reset! delete-by-backspace true)))

                :on-change   (fn [e] (reset! team-name-state (.. e -target -value)))}]])))


(defn team-row [handle {:keys [team-name] :as ls} {:keys [hook/team] :as fs} index]
  [:div {:style {:display :flex :align-items :center :min-height 30}}
   [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc index)]
   [:input {:id          (rc/id handle "team-name")
            :style       (merge s/input-text-field {:min-width 200})

            ;take from local state first
            :value       (if team-name team-name (:team-name team))

            :on-change   (fn [e] (->> e ut/value (rc/put! handle assoc :team-name)))
            :on-key-down (ut/key-handler {})
            :on-key-up   (ut/key-handler {[:BACKSPACE]    (fn [])
                                          [:ENTER]        (fn [] [:STOP-PROPAGATION])
                                          [:SHIFT :ENTER] (fn [] [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                          [:UP]           (fn [])
                                          [:DOWN]         (fn [])
                                          :else           (fn [])})}]])


(defn render [{:keys [ctx id] :as handle} state {:keys [hook/teams-order hook/teams]}]
  (let [{:keys [scroll-top
                scroll-height
                client-height]} state]
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
             ^{:key team-id} [rc/build (merge ctx {:team-id team-id}) :hook/ui-team-row index]) teams-order (range (count teams)))]

     ; input field
     [:div {:style {:padding-left 120 :padding-bottom 20}}
      [rc/build ctx :hook/ui-enter-team-input]]]))