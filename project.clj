(defproject bracketbird "0.1.0-SNAPSHOT"
  :description "Tournament Manager"
  :url "http://www.bracketbird.com"
  :dependencies [[org.clojure/clojure "1.10.0"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.516"]
                                  [com.bhauman/figwheel-main "0.2.3"]
                                  ;; optional but recommended
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]

                                  [reagent "0.8.1"]
                                  [reagent-utils "0.3.3"]]

                   :resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]}}

  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]})
