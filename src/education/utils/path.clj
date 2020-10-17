(ns education.utils.path
  (:require [clojure.string :as str]))

(defn- strip-prefix-slash
  [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))

(defn- strip-suffix-slash
  [s]
  (if (str/ends-with? s "/")
    (subs s 0 (dec (count s)))
    s))

(defn- join-two-paths
  [a b]
  (str/join "/" [(strip-suffix-slash (strip-prefix-slash a))
                 (strip-suffix-slash (strip-prefix-slash b))]))

(defn join
  [base & args]
  (str/join "/" [(strip-suffix-slash base) (reduce join-two-paths args)]))
