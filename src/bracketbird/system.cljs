(ns bracketbird.system
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [bracketbird.state :as state]
            [bracketbird.util :as ut]
            [bracketbird.application :as application]
            [bracketbird.event-dispatcher :as event-dispatcher]))

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
                                                          (= (.. js/window -location -hash) "#test"))
                                    :in                 (event-dispatcher/init-in-channel)
                                    :out                (event-dispatcher/init-out-channel)})


  ;has to be done after test is set
  (let [app-id (unique-id :application)]
    (swap! state/state assoc-in [:system :active-application] app-id)
    (swap! state/state assoc-in [:applications app-id] (application/mk-application app-id))))



