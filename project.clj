(defproject bracketbird "0.1.0-SNAPSHOT"
  :description "Tournament Manager System"
  :url "http://www.bracketbird.com"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.1"]
                 [noencore "0.1.20"]
                 [cljs-ajax "0.5.1" :exclusions [org.clojure/clojurescript]]
                 ;[funcool/promesa "0.4.0"]
                 ;[com.andrewmcveigh/cljs-time "0.3.13"]
                 ;[bidi "1.25.0" :exclusions [prismatic/schema]]
                 ]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-2" :exclusions [ring/ring-core org.clojure/clojure org.clojure/tools.reader]]]

  :hooks [leiningen.cljsbuild]
  :source-paths ["src" "dev"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "resources/private/js/compiled"]

  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.0-2" :exclusions [org.clojure/core.async org.clojure/data.priority-map org.codehaus.plexus/plexus-utils joda-time]]]}}

  :cljsbuild {
              :builds [{:id           "dev"
                        :source-paths ["src" "dev"]

                        :figwheel     {:on-jsload "bracketbird.core/on-js-reload"}

                        :compiler     {:main                 bracketbird.core
                                       :asset-path           "js/compiled/out"
                                       :output-to            "resources/public/js/compiled/bracketbird.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true}}]}

  :figwheel {})
