(defproject poker "0.1.0-SNAPSHOT"
  :description "poker your stories"
  :url "http://poker.club"
  :min-lein-version "2.3.3"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "0.5.527"]
                 [compojure "1.6.1"]
				 [ring "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [http-kit "2.3.0"]
                 [reagent "0.10.0"]
                 [reagent-utils "0.3.3"]
                 [com.taoensso/sente "1.15.0"]
                 [metosin/reitit "0.4.2"]
                 [pez/clerk "1.0.0"]
				 [danlentz/clj-uuid "0.1.9"]
				 [digest "1.4.9"]
                 [venantius/accountant "0.2.5"
                  :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/timbre "4.10.0"]]
  :plugins [[lein-nsorg "0.3.0"]
            [lein-pprint         "1.2.0"]
            [lein-ancient        "0.6.15"]
            [lein-cljfmt "0.6.7"]
            [lein-cljsbuild      "1.1.7"]]

  :hooks [leiningen.cljsbuild]
  :main poker.clj.main
  :aot [poker.clj.main]

  :cljsbuild
  {:builds
   [{:id :cljs-client
    :source-paths ["src/poker/cljs"]
    :compiler {:output-to "resources/public/main.js"
               :optimizations :whitespace #_:advanced
               :pretty-print true}}]}

  :clean-targets ^{:protect false} ["resources/public/main.js"]

;;  :resource-paths ["resources/public"]

  :profiles
 {:uberjar {:aot :all
            :cljsbuild {:builds [{
    :source-paths ["src/poker/cljs"]
    :compiler {:output-to "resources/public/main.js"
               :optimizations :whitespace #_:advanced
               :pretty-print true}}]}} 
  :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
  :aliases
  {"start" ["do" "clean," "cljsbuild", "once," "ring"]}
