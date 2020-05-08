(defproject ring_learn "0.1.0-SNAPSHOT"
  :description "Just simple hello world project to learn ring"
  :url "http://example.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aero "1.1.6"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-mock "0.4.0"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 ;; [metosin/ring-swagger-ui "3.25.0"]
                 [ragtime "0.8.0"]
                 [seancorfield/next.jdbc "1.0.424"]
                 [org.postgresql/postgresql "42.2.10"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [com.stuartsierra/dependency "0.2.0"]
                 [com.stuartsierra/component "0.4.0"]
                 ;; Security0
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-sign "3.1.0"]
                 [buddy/buddy-auth "2.2.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.10.3"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]]
  :repl-options {:init-ns ring-learn.core}
  :aliases {"migrate"  ["run" "-m" "ring-learn.database.component/migrate" "--"]
            "rollback" ["run" "-m" "ring-learn.database.component/rollback" "--"]}
  :uberjar-name "education-api-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles {:uberjar {:aot :all}}
  :main ring-learn.core)
