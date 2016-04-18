(ns dev
    (:use [figwheel-sidecar.repl-api]))

(defn start-figwheel
      ([]
        (start-figwheel 3449))
      ([server-port]
        (start-figwheel! {:figwheel-options {:server-port server-port
                                             :css-dirs ["resources/public/css"]}
                          :all-builds (figwheel-sidecar.repl/get-project-cljs-builds)})
        (cljs-repl)))

(dev/start-figwheel)
