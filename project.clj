(defproject ring_learn "0.1.0-SNAPSHOT"
  :description "Just simple hello world project to learn ring"
  :url "http://example.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]
                 [cheshire "5.9.0"]
                 [http-kit "2.3.0"]]
  :repl-options {:init-ns ring-learn.core}
  :main ring-learn.core)
