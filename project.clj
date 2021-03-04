(defproject education-api "1.4.0"
  :description "Back end for education web application"
  :url "http://educationapp-api.herokuapp.com/swagger"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; Configuration file parsing
                 [aero "1.1.6"]
                 ;; Human readable errors
                 [phrase "0.3-alpha4"]
                 ;; Ring and compojure
                 [ring/ring-defaults "0.3.2"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 ;; [metosin/spec-tools "0.10.3"]
                 [ring-cors "0.1.13"]
                 [http-kit "2.3.0"]
                 ;; No support for OpenAPI 3.0
                 ;; [metosin/ring-swagger-ui "3.25.0"]
                 ;; Database stuff
                 [ragtime "0.8.0"]
                 [seancorfield/next.jdbc "1.1.588"]
                 [org.postgresql/postgresql "42.2.14"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [honeysql "0.9.10"]
                 ;; Integrant framework
                 [integrant "0.8.0"]
                 ;; Security
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-sign "3.1.0"]
                 [buddy/buddy-auth "2.2.0"]
                 ;; Logging
                 [com.taoensso/timbre "5.1.1"]
                 ;; JSON parsing
                 [com.fasterxml.jackson.core/jackson-core "2.11.1"]]
  :repl-options {:init-ns education.core}
  :aliases {"migrate"  ["run" "-m" "education.database.migrations/migrate" "--"]
            "rollback" ["run" "-m" "education.database.migrations/rollback" "--"]}
  :uberjar-name "education-api-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[tortue/spy "2.0.0"]
                                  [ring/ring-mock "0.4.0"]
                                  [peridot "0.5.3"]
                                  [org.clojure/test.check "1.0.0"]]}
             :uberjar {:aot :all}}
  :plugins [[lein-cloverage "1.1.2"]
            [lein-cljfmt "0.7.0"]]
  :main education.core)
