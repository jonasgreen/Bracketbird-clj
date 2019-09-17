(ns bracketbird.doku)



(def hook-team [:application :tournaments :tournament-id :teams :team-id])

(def state {:application {:tournaments {"uid-1" {:teams       {"uid-2" {:team-name "Barcelona"}
                                                               "uid-3" {:team-name "Real Madrid"}}
                                                 :teams-order ["uid-2" "uid-3"]}

                                        "uid-4" {:teams       {"uid-5" {:team-name "Liverpool"}
                                                               "uid-6" {:team-name "Chelsea"}}
                                                 :teams-order ["uid-6" "uid-5"]}}}})

