(ns bracketbird.model.tournament)


(def id-key :id)
(def state-key :state)

(def url-key :url)
(def url-view-key :url-view)
(def channel-id-key :channel-id)

(def stages-key
  "A tournament is made of one or more stages, ie. group-play followed by knockout-play."
  :stages)

(def teams-key
  "Teams participating"
  :teams)

(def final-ranking-key
  "Final raning of the tournament. format: [[team1][team2][team3 team4]...]"
  :final-ranking)


(defn add-team [team])
