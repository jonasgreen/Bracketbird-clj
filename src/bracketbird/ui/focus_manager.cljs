(ns bracketbird.ui.focus-manager)


(defprotocol IFocusManager
  (-up [this src])
  (-down [this src])
  (-left [this src])
  (-right [this src]))
