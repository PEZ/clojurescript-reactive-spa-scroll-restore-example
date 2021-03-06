(defproject scroll-restore-example "0.1.0-SNAPSHOT"
  :description "Scroll restoration example for clojurescript reactive SPA:s"
  :url "https://github.com/PEZ/clojurescript-reactive-spa-scroll-restore-example"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.9.0-beta3"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.443"
                  :exclusions [org.clojure/tools.reader]]
                 [reagent "0.8.0-alpha2"]
                 [reagent-utils "0.2.1"]
                 [bidi "2.1.2"]
                 [venantius/accountant "0.2.0"]]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies [[figwheel-sidecar "0.5.14" :exclusions [org.clojure/clojure]]
                             [com.cemerick/piggieback "0.2.2" :exclusions [org.clojure/clojure]]
                             [prismatic/schema "1.1.7"]]}}


  :cljsbuild {:builds
              {:dev
               {:source-paths ["src"]

                :figwheel {:on-jsload "scroll-restore.core/on-js-reload"
                           :websocket-host :js-client-host}


                :compiler {:main scroll-restore.core
                           :asset-path "/js/compiled/out"
                           :output-to "resources/public/js/compiled/scroll-restore-example.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}

               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               :min
               {:source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/scroll-restore-example.js"
                           :main scroll-restore.core
                           :optimizations :advanced
                           :pretty-print false}}}}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :server-ip "0.0.0.0"
             :css-dirs ["resources/public/css"]
             :ring-handler scroll-restore.server/handler}

  :repl-options {:init-ns scroll-restore.user
                 :skip-default-init false
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]})
