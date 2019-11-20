(ns recontain.impl.call-stack)


(defn mk [local-state foreign-states comp-configs]
  {:local-state    local-state
   :foreign-states foreign-states
   :comp-configs   comp-configs})
