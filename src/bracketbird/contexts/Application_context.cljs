(ns bracketbird.contexts.application-context
  (:require [bracketbird.app-state :as app-state]
            [bracketbird.contexts.context :as context]))


(def application-context
  (reify
    context/IContext

    (-state [this] app-state/state)
    (-id [this])
    (-root-path [this])
    (-load [this])
    (-unload [this])))


(defn subscribe-navigation []
  (context/subscribe application-context [:navigation]))


(defn update-navigation [token ctx]
  #_(context/update (fn[d] (assoc d :navigation token)))
  (assoc-in (context/get-data application-context) [:navigation] token)

  )

(defn subscriber-user []
  (context/subscribe application-context [:user]))

(defn subscribe-config []
  (context/subscribe application-context [:configuration]))


(defn create-tournament []
  ;initialize context
  ;init router
  ;update-navigation
  )

(defn load-tournament [token])
