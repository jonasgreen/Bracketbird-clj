(ns bracketbird.application-state
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]))


(defonce state (r/atom {}))

