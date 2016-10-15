(ns bracketbird.pages.teams-tab
  (:require [bracketbird.ui.styles :as s]
            [bracketbird.context :as context]
            [bracketbird.model.team :as t]
            [bracketbird.util.utils :as ut]
            [airboss.utils :as k]
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
                             :update (fn [t team-name])
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

(defn enter-team-panel [ctx _ teams-count]
  (let [team-name (context/subscribe-ui ctx)]
    (fn [ctx dispatcher]
      [:div {:style {:display :flex :padding-left 30 :align-items :center}}
       [:input {:placeholder "Enter team"
                :id          (ut/dom-id-from-ui-ctx ctx :enter-team)
                :type        :text
                :style       s/input-text-field
                :value       @team-name
                :on-key-down #(when (k/key? :UP %) (dispatcher [:enter-team :up]))
                :on-key-up   #(when (k/key? :ENTER %) (dispatcher [:enter-team :create-team] @team-name))
                :on-change   (context/update-ui-on-input-change! ctx)}]

       [:button {:class "primaryButton"} "Add Team"]])))


(defn team-name-panel [])

(defn team-panel [position team _]
  (let [team-name-state (r/atom (t/team-name team))
        delete-by-backspace (atom (clojure.string/blank? (t/team-name team)))]
    (r/create-class
      {:reagent-render (fn [position team dispatcher]

                         [:div {:style {:display     :flex
                                        :align-items :center
                                        :min-height  30}}
                          [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc position)]
                          [:input {:id          (ut/dom-id-from-entity team :team-name)
                                   :style       s/input-text-field
                                   :value       @team-name-state
                                   :on-key-down (fn [e] (cond (and (k/key? :BACKSPACE e) @delete-by-backspace)
                                                              (dispatcher [:team-name :delete] team)

                                                              (k/key? :DOWN e)
                                                              (dispatcher [:team-name :down] team)

                                                              (k/key? :UP e)

                                                              (dispatcher [:team-name :up] team)))

                                   :on-key-up   (fn [e] (when (clojure.string/blank? @team-name-state)
                                                          (reset! delete-by-backspace true)))

                                   :on-change   (fn [e] (reset! team-name-state (.. e -target -value)))}]])})))

(defn teams-panel [ctx _]
  (let [teams (t-ctrl/subscribe-teams ctx)]
    (fn [_ dispatcher]
      [:div
       (map-indexed (fn [i t] (ut/r-key t [team-panel i t dispatcher])) @teams)])))

(defn content [ctx]
  (let [enter-team-ctx (context/sub-ui-ctx ctx [:enter-team])
        dispatcher (component-dispatcher ctx enter-team-ctx)]

    (fn [ctx] [:div
               [teams-panel ctx dispatcher]
               [enter-team-panel enter-team-ctx dispatcher]])))

(defn render [ctx]
  [tab-content/render ctx [content ctx]])