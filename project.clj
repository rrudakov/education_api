(defproject education-api "1.4.0-SNAPSHOT"
  :description "Back end for education web application"
  :url "http://educationapp-api.herokuapp.com/swagger"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
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
                 [com.github.seancorfield/next.jdbc "1.1.643"]
                 [org.postgresql/postgresql "42.2.14"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [com.github.seancorfield/honeysql "2.0.0-alpha3"]
                 ;; Integrant framework
                 [integrant "0.8.0"]
                 ;; Security
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-sign "3.1.0"]
                 [buddy/buddy-auth "2.2.0"]
                 ;; Logging
                 [com.taoensso/timbre "5.1.1"]
                 ;; HTTP client
                 [clj-http "3.10.3"]
                 ;; Content-type negotiation
                 [metosin/muuntaja "0.6.8"]]
  :repl-options {:init-ns education.core}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :aliases {"migrate"  ["run" "-m" "education.database.migrations/migrate" "--"]
            "rollback" ["run" "-m" "education.database.migrations/rollback" "--"]}
  :uberjar-name "education-api-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:dev
             {:resource-paths ["test-resources"]
              :dependencies   [[tortue/spy "2.4.0"]
                               [ring/ring-mock "0.4.0"]
                               ;; Create multipart entity (consider removing this)
                               [peridot "0.5.3"]]}
             :uberjar
             {:aot :all}}
  :plugins [[lein-cloverage "1.1.2"]
            [lein-cljfmt "0.7.0"]]
  :main education.core)
