(ns bracketbird.model.entity)


(defprotocol IEntity
  (-id [this]))
