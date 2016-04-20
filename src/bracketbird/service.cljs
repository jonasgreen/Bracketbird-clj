(ns bracketbird.service)

;events




(defprotocol IService
  (-execute-event [this event]))



(deftype Mock-backend []
  IService
  (-execute-event [this event]))
