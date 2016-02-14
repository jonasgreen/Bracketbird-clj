(ns bracketbird.model.event)


(defn- squuid-seconds-component
  "Returns the current time rounded to the nearest second."
  []
  (-> (.now js/Date)
      (/ 1000)
      (Math/round)))

(defn squuid
  "Constructs a semi-sequential UUID. Useful for creating UUIDs that
   don't fragment indexes. Returns a UUID whose most significant 32
   bits are the current time in milliseconds, rounded to the nearest
   second."
  []
  (let [seconds-hex (.toString (squuid-seconds-component) 16)
        trailing (.replace "-xxxx-4xxx-yxxx-xxxxxxxxxxxx" (js/RegExp. "[xy]" "g")
                           (fn [c]
                             (let [r (bit-or (* 16 (Math/random)) 0)
                                   v (if (= c "x") r (bit-or (bit-and r 0x3) 0x8))]
                               (.toString v 16))))]
    (uuid (str seconds-hex trailing))))

(defn squuid-time-millis
  "Get the time part of a squuid."
  [squuid]
  (-> (.-uuid squuid)
      (.slice 0 8)
      (js/parseInt 16)
      (* 1000)))