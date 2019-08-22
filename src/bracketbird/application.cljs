(ns bracketbird.application
  (:require [bracketbird.state :as state]
            [bracketbird.util :as ut]))


(defn mk-application [id]
  {:application-id id
   :active-page    :front-page
   :tournament     {}
   :tournament-events []})