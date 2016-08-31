(ns bracketbird.util.keyboard)







; ------- Keys --------

(def F10 121)
(def F12 123)
(def ESC 27)

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)

(def ALT 18)
(def ENTER 13)
(def BACKSPACE 8)
(def SPACE 32)
(def DELETE 46)


(def CHAR_0 48)
(def CHAR_9 57)
(def CHAR_V 86)



; ------- key utils ----

(defn key-code [event]
  (when event (.-keyCode event)))

(defn key? [keycode event]
  (= keycode (key-code event)))

(defn F10? [event]
  (key? F10 event))

(defn F12? [event]
  (key? F12 event))

(defn esc? [event]
  (key? ESC event))

(defn arrow-down? [event]
  (key? ARROW_DOWN event))

(defn arrow-up? [event]
  (key? ARROW_UP event))

(defn arrow-right? [event]
  (key? ARROW_RIGHT event))

(defn arrow-left? [event]
  (key? ARROW_LEFT event))


(defn alt? [event]
  (key? ALT event))

(defn space? [event]
  (key? SPACE event))

(defn enter? [event]
  (key? ENTER event))

(defn delete? [event]
  (key? DELETE event))

(defn backspace? [event]
  (key? BACKSPACE event))


(defn char-0? [event]
  (key? CHAR_0 event))

(defn char-9? [event]
  (key? CHAR_9 event))

(defn char-v? [event]
  (key? CHAR_V event))

(defn some-key [event & keys]
  (some #(key? % event) keys))

(defn shift-modifyer? [event]
  (when event (.-shiftKey event)))

(defn not-shift-modifyer? [event]
  (not (shift-modifyer? event)))

(defn ctrl-modifyer? [event]
  (when event (.-ctrlKey event)))

(defn alt-modifyer? [event]
  (when event (.-altKey event)))

(defn satisfy-all
  "Returns true if all conditions return true when they are passed the key event event.
  Use like this (when (k/satisfy-all e k/arrow-down? k/not-shift-modifyer?))"
  ([event condition]
   (condition event))
  ([event condition & cs]
   (let [ret (satisfy-all event condition)]
     (if (and ret (seq cs))
       (recur event (first cs) (rest cs))
       ret))))
