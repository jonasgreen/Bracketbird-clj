(ns bracketbird.system
  (:require [bracketbird.state :as state]))

(defonce test-ids (atom {}))


(defn squuid
  "Constructs a semi-sequential UUID. Useful for creating UUIDs that
   don't fragment indexes. Returns a UUID whose most significant 32
   bits are the current time in milliseconds, rounded to the nearest
   second."
  []
  (let [current-time-nearest-second (-> (.now js/Date)
                                        (/ 1000)
                                        (Math/round))
        seconds-hex (.toString current-time-nearest-second 16)
        trailing (.replace "-xxxx-4xxx-yxxx-xxxxxxxxxxxx" (js/RegExp. "[xy]" "g")
                           (fn [c]
                             (let [r (bit-or (* 16 (Math/random)) 0)
                                   v (if (= c "x") r (bit-or (bit-and r 0x3) 0x8))]
                               (.toString v 16))))]
    (uuid (str seconds-hex trailing))))



(defn test? [] (get-in @state/state [:system :test]))

(defn unique-id [k]
  (if (test?)
    (-> test-ids
        (swap! update k inc)
        (get k))
    (squuid)))


(defn mk-application [id]
  {:application-id    id
   :active-page       :front-page
   :tournament        {}
   :tournament-events []})




