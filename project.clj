(defproject bracketbird "0.1.0-SNAPSHOT"
  :description "Tournament manager"
  :url "http://bracketbird.com"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/schema "1.0.1"]
                 [noencore "0.1.20"]
                 [reagent "0.5.1"]
                 [cljs-ajax "0.5.1" :exclusions [org.clojure/clojurescript]]
                 [funcool/promesa "0.4.0"]
                 [com.andrewmcveigh/cljs-time "0.3.13"]
                 [bidi "1.25.0" :exclusions [prismatic/schema]]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-2" :exclusions [ring/ring-core org.clojure/clojure org.clojure/tools.reader]]
            [lein-doo "0.1.6"]]
  :hooks [leiningen.cljsbuild]
  :profiles {:dev  {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                   [figwheel-sidecar "0.5.0-2" :exclusions [org.clojure/core.async org.clojure/data.priority-map org.codehaus.plexus/plexus-utils joda-time]]
                                   [lein-doo "0.1.6"]]
                    :source-paths ["cljs_src" "dev"]}
             :repl {:plugins [[cider/cider-nrepl "0.10.0"]]}}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :source-paths ["src"]
  :doo {:build "test"
        :paths {:phantom "./bin/phantomjs"}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "resources/private/js/compiled"]

  :cljsbuild {
              :test-commands
                      {"unit-tests"
                       ["./bin/phantomjs" "resources/private/js/compiled/unit-test.js"]}
              :builds [{:id           "dev"
                        :source-paths ["src" "src-common"]

                        :figwheel     {:on-jsload "bracketbird.core/on-js-reload"}

                        :compiler     {:main                 bracketbird.core
                                       :asset-path           "js/compiled/out"
                                       :output-to            "resources/public/js/compiled/bracketbird.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true}}

                       {:id           "deploy"
                        :source-paths ["src" "src-common"]
                        :compiler     {:main          bracketbird.core
                                       :output-dir    "resources/public/js/compiled/deploy_out"
                                       :output-to     "resources/public/js/compiled/bracketbird_deploy.js"
                                       :asset-path    "js/compiled/deploy_out"
                                       :source-map    "resources/public/js/compiled/deploy.js.map"
                                       :language-in   :ecmascript5 ;; Mute warnings re: promesa outputting non-ES3 compliant javascript
                                       :language-out  :ecmascript5
                                       :optimizations :whitespace}}

                       {:id           "test"
                        :source-paths ["src-common" "test"]
                        :compiler     {:output-to     "resources/private/js/compiled/unit-test.js"
                                       :optimizations :whitespace
                                       :pretty-print  true}}]}

  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"]             ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             })
