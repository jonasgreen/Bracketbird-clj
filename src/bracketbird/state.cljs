(ns bracketbird.state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))


(defonce state (r/atom {}))


(defn subscribe
  ([path] (subscribe path nil))
  ([path not-found] (reaction (get-in @state path not-found))))