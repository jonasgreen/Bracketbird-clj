(ns bracketbird.components.teams-tab
  (:require [bracketbird.styles :as s]
            [bracketbird.state :as state]
            [bracketbird.ui-services :as ui-service]
            [bracketbird.dom :as d]
            [goog.dom :as dom]
            [reagent.core :as r]))


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



(defn enter-team-input [ctx]
  (let [team-name (r/atom "")
        dom-id (state/dom-id ctx :enter-team-input)
        create-team (fn []
                      (ui-service/dispatch-event [:team :create] ctx {:team-name @team-name})
                      (reset! team-name ""))

        key-down-handler (d/handle-key {:ENTER create-team})]

    (fn [_]
      [:div {:style {:display     :flex
                     :margin-top  30
                     :align-items :center}}
       [:input {:placeholder "Enter team"
                :id          dom-id
                :type        :text
                :style       s/input-text-field
                :value       @team-name
                :on-key-down key-down-handler
                :on-change   (fn [e] (reset! team-name (.. e -target -value)))}]

       [:button {:class    "primaryButton"
                 :on-click create-team
                 } "Add Team"]])))


(defn team-row_old [position ctx]
  (let [team (state/hook :hooks/team ctx)
        team-name-state (r/atom (:team-name team))
        delete-by-backspace (atom (clojure.string/blank? (:team-name team)))]
    (fn [position _]

      [:div {:style {:display     :flex
                     :align-items :center
                     :min-height  30}}
       [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc position)]
       [:input {:id          (hash (conj (state/hook-path :hooks/teams-tab ctx) :input))
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

                                     (d/key? :ENTER e)
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


(defn team-row [ctx]
  (fn [_]
    (let [team (state/hook :hooks/team ctx)]
      [:div (:team-name @team)])))


(defn render [ctx {:keys [values] :as opts}]
  (let [teams-order (:hooks/teams-order opts)]
    [:div {:style    (merge
                       {:display        :flex
                        :flex-direction :column
                        :height         :100%
                        ;:background     :red
                        }
                       (when (< 0 (:scroll-top values)) {:border-top "1px solid rgba(241,241,241,1)"}))

           :on-click (fn [e] ())}


     ; teams table
     [:div {:id        (:dom-id values)
            :style     (merge {:padding-top    40
                               :padding-left   120
                               :max-height     :100%
                               :min-height     :200px
                               :padding-bottom 40
                               :overflow-y     :auto
                               ;:background   :yellow
                               } (when (not= (+ (:scroll-top values) (:client-height values))
                                             (:scroll-height values)) {:border-bottom "1px solid rgba(241,241,241,1)"}))
            :on-scroll (fn [e]
                         (let [target (.-target e)]
                           (state/put! values
                                       :scroll-top (.-scrollTop target)
                                       :scroll-height (.-scrollHeight target)
                                       :client-height (.-clientHeight target))))}
      (map (fn [team-id]
             ^{:key team-id} [team-row (assoc ctx :team-id team-id)]) teams-order)]

     ; input field
     [:div {:style {:padding-left   120
                    :padding-bottom 20
                    ;:background   :orange
                    }}
      [enter-team-input ctx]]]))