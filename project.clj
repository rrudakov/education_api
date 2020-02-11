(defproject ring_learn "0.1.0-SNAPSHOT"
  :description "Just simple hello world project to learn ring"
  :url "http://example.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-mock "0.4.0"]
                 [metosin/compojure-api "2.0.0-alpha30"]
                 [ragtime "0.8.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.postgresql/postgresql "42.2.10"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [com.stuartsierra/dependency "0.2.0"]
                 [com.stuartsierra/component "0.4.0"]
                 [compojure "1.6.1"]
                 [cheshire "5.9.0"]
                 [http-kit "2.3.0"]]
  :repl-options {:init-ns ring-learn.core}
  :aliases {"migrate"  ["run" "-m" "ring-learn.database.component/migrate"]
            "rollback" ["run" "-m" "ring-learn.database.component/rollback"]}
  :main ring-learn.core)
