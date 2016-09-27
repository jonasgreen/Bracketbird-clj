(ns bracketbird.pages.teams-tab
  (:require [bracketbird.ui.styles :as s]
            [bracketbird.context :as context]
            [bracketbird.model.team :as t]
            [bracketbird.util.utils :as ut]
            [bracketbird.util.keyboard :as k]
            [bracketbird.tournament-controller :as t-ctrl]
            [bracketbird.pages.tournament-tab-content :as tab-content]
            [reagent.core :as r]))

(defn move-entity-focus-up [entities entity sub-key]
  (-> (ut/previous-entity entities entity)
      (ut/focus-by-entity sub-key)))

(defn move-entity-focus-down [entities entity sub-key]
  (-> (ut/next-entity entities entity)
      (ut/focus-by-entity sub-key)))

(defn component-dispatcher [ctx enter-team-ctx]
  (let [config {:team-name  {:up     (fn [t] (move-entity-focus-up (t-ctrl/teams ctx) t :team-name))
                             :down   (fn [t] (when-not (move-entity-focus-down (t-ctrl/teams ctx) t :team-name)
                                               (ut/focus-by-ui-ctx enter-team-ctx :enter-team)))

                             :left   (fn [t] (ut/focus-by-entity t :team-id))
                             :update (fn[t team-name])
                             :delete (fn [t] (t-ctrl/delete-team ctx t))}

                :team-id    {:up    (fn [t])
                             :down  (fn [t])
                             :right (fn [t] (ut/focus-by-entity t :team-name))}

                :enter-team {:up          (fn []
                                            (when-let [last-team (t-ctrl/last-team ctx)]
                                              (ut/focus-by-entity last-team :team-name)))

                             :create-team (fn [team-name]
                                            (t-ctrl/add-team ctx team-name)
                                            (context/update-ui! enter-team-ctx ""))}}]

    (fn [path & args]
      (if-let [f (get-in config path)]
        (apply f args)
        (.warn js/console "Unable to dispatch " path " with " args)))))

(defn enter-team-panel [ctx _]
  (let [team-name (context/subscribe-ui ctx)]
    (fn [ctx dispatcher]
      [:div {:style {:display :flex :padding-left 30 :align-items :center :padding-top 30}}
       [:input {:placeholder "Enter team"
                :id          (ut/dom-id-from-ui-ctx ctx :enter-team)
                :type        :text
                :style       s/input-text-field
                :value       @team-name
                :on-key-down #(when (k/arrow-up? %) (dispatcher [:enter-team :up]))
                :on-key-up   #(when (k/enter? %) (dispatcher [:enter-team :create-team] @team-name))
                :on-change   (context/update-ui-on-input-change! ctx)}]

       [:button {:class "primaryButton"} "Add Team"]])))


(defn team-name-panel [])

(defn team-panel [_ team _]
  (let [team-name-state (r/atom (t/team-name team))
        delete-by-backspace (atom (clojure.string/blank? (t/team-name team)))]
    (r/create-class
      {:reagent-render      (fn [position team dispatcher]
                              (println "2")

                              [:div {:style {:display     :flex
                                             :align-items :center
                                             :position    :absolute
                                             :top         (* position 30)
                                             ;:transition  "top 2s linear"
                                             :min-height  30}}
                               [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc position)]
                               [:input {:id          (ut/dom-id-from-entity team :team-name)
                                        :style       s/input-text-field
                                        :value       @team-name-state
                                        :on-key-down (fn [e] (cond (and (k/backspace? e) @delete-by-backspace)
                                                                   (dispatcher [:team-name :delete] team)

                                                                   (k/arrow-down? e)
                                                                   (dispatcher [:team-name :down] team)

                                                                   (k/arrow-up? e)
                                                                   (dispatcher [:team-name :up] team)))

                                        :on-key-up   (fn [e] (when (clojure.string/blank? @team-name-state)
                                                               (reset! delete-by-backspace true)))

                                        :on-change   (fn [e] (reset! team-name-state (.. e -target -value)))}]])

       :component-did-mount (fn [this]
                              #_(.info js/console (.-style (r/dom-node this)))
                              #_(.setProperty (.-style (r/dom-node this)) "top" (+ 30 (* position 30))))
       })))

(defn teams-panel [ctx _]
  (let [teams (t-ctrl/subscribe-teams ctx)]
    (fn [ctx dispatcher]
      [:div {:style {:position :relative :min-height (* 30 (count @teams))}}
       (map-indexed (fn [i t] (ut/r-key t [team-panel i t dispatcher])) @teams)])))

(defn content [ctx]
  (let [enter-team-ctx (context/sub-ui-ctx ctx [:enter-team])
        dispatcher (component-dispatcher ctx enter-team-ctx)]

    (fn [ctx] [:div
               [teams-panel ctx dispatcher]
               [enter-team-panel enter-team-ctx dispatcher]])))

(defn render [ctx]
  [tab-content/render ctx [content ctx]])