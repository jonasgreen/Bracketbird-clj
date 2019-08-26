(ns bracketbird.system
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bracketbird.application :as application]
            ))

(defonce test-ids (atom {}))


(defn test? [] (get-in @state/state [:system :test]))

(defn unique-id [k]
  (if (test?)
    (-> test-ids
        (swap! update k inc)
        (get k))
    (ut/squuid)))


(defn initialize! []
  (swap! state/state assoc :system {:active-application nil
                                    :figwheel-reloads   0
                                    :context-keys       state/context-paths
                                    :test               (or
                                                          (= (.. js/window -location -hostname) "localhost")
                                                          (= (.. js/window -location -hash) "#test"))})

  ;has to be done after test is set
  (let [app-id (unique-id :application)]
    (swap! state/state assoc-in [:system :active-application] app-id)
    (swap! state/state assoc-in [:applications app-id] (application/mk-application app-id))))



