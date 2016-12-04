(defproject bracketbird "0.1.0-SNAPSHOT"
  :description "Tournament Manager"
  :url "http://www.bracketbird.com"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [noencore "0.2.0"]
                 [reagent "0.6.0"]
                 [cljs-ajax "0.5.4"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [reagent-utils "0.1.8"]
                 [cljs-http "0.1.40"]
                 [bidi "2.0.9" :exclusions [clj-time]]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.7"]
            [lein-codox "0.9.0"]
            [lein-doo "0.1.6" :exclusions [org.clojure/tools.reader]]]

  :hooks [leiningen.cljsbuild]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :source-paths ["src" "dev"]

  :doo {:build "test"
        :paths {:phantom "./bin/phantomjs"}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "resources/private/js/compiled"]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.7"]
                                  [lein-doo "0.1.6"]]}}

  :cljsbuild {
              :builds [{:id           "dev"
                        :source-paths ["src" "dev"]
                        :figwheel     {:on-jsload "bracketbird.core/on-js-reload"}
                        :compiler     {:main                 bracketbird.core
                                       :asset-path           "js/compiled/out"
                                       :output-to            "resources/public/js/compiled/bracketbird.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true}}


                       {:id           "deploy"
                        :source-paths ["src"]
                        :compiler     {:main          bracketbird.core
                                       :output-dir    "resources/public/js/compiled/deploy_out"
                                       :output-to     "resources/public/js/compiled/bracketbird_deploy.js"
                                       :asset-path    "js/compiled/deploy_out"
                                       :source-map    "resources/public/js/compiled/deploy.js.map"
                                       :language-in   :ecmascript5 ;; Mute warnings re: promesa outputting non-ES3 compliant javascript
                                       :language-out  :ecmascript5
                                       :optimizations :simple}}

                       {:id           "test"
                        :source-paths ["test"]
                        :compiler     {:output-to    "resources/private/js/compiled/unit-test.js"
                                       :pretty-print true}}
                       ]}
  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"]})
