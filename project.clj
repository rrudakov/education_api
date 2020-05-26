(defproject education-api "0.1.0-SNAPSHOT"
  :description "Back end for education web application"
  :url "http://educationapp-api.herokuapp.com/swagger"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; Configuration file parsing
                 [aero "1.1.6"]
                 ;; Ring and compojure
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-mock "0.4.0"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 ;; [metosin/spec-tools "0.10.3"]
                 [ring-cors "0.1.13"]
                 [http-kit "2.3.0"]
                 ;; No support for OpenAPI 3.0
                 ;; [metosin/ring-swagger-ui "3.25.0"]
                 ;; Database stuff
                 [ragtime "0.8.0"]
                 [seancorfield/next.jdbc "1.0.445"]
                 [org.postgresql/postgresql "42.2.10"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [honeysql "0.9.10"]
                 ;; Component framework
                 [com.stuartsierra/dependency "0.2.0"]
                 [com.stuartsierra/component "0.4.0"]
                 ;; Security
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-sign "3.1.0"]
                 [buddy/buddy-auth "2.2.0"]
                 ;; JSON parsing
                 [com.fasterxml.jackson.core/jackson-core "2.10.3"]]
  :repl-options {:init-ns education.core}
  :aliases {"migrate"  ["run" "-m" "education.database.component/migrate" "--"]
            "rollback" ["run" "-m" "education.database.component/rollback" "--"]}
  :uberjar-name "education-api-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:uberjar {:aot :all}}
  :main education.core)
