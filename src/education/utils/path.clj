(ns education.utils.path
  (:require
   [clojure.string :as str]))

(defn- strip-prefix-slash
  [s]
  (->> s
       str/trim
       (drop-while #{\/})
       (apply str)))

(defn- strip-suffix-slash
  [s]
  (->> s
       str/trim
       reverse
       (drop-while #{\/})
       reverse
       (apply str)))

(defn- join-two-paths
  [a b]
  (str/join "/" [(strip-suffix-slash a) (strip-prefix-slash b)]))

(defn join
  [base & args]
  (reduce join-two-paths (strip-suffix-slash base) args))
