(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'io.github.rrudakov/education-api)
(def version (format "1.6.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "target/education-api-standalone.jar")

(defn clean [_]
  (println "Clean target")
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'education.core})
  (println "Created" uber-file))

(defn release [_]
  (println "Release version" version)
  (b/process {:command-args ["git" "tag" "--sign" version "-a" "-m" (str "Release " version)]})
  (println "Success!"))
