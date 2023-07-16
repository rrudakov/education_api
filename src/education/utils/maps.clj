(ns education.utils.maps
  (:require
   [clojure.string :as str]))

(defn unqualify-keyword
  "Take pair of qualified key and value and produce map entry with unqualified
  keys."
  [[k v]]
  [(keyword (name k)) v])

(defn unqualify-map
  "Take map `m` with qualified keys and produce new one with unqualified keys."
  [m]
  (apply hash-map (mapcat unqualify-keyword m)))

(defn update-if-exist
  "Apply function `f` to value associated with key `k` if `k` is present in the map `m`."
  [m k f]
  (if (contains? m k) (update m k f) m))

(defn ->camel-key
  [[k v]]
  (let [match->upper-case (fn [[_ s]] (str/upper-case s))
        new-key (-> (name k)
                    (str/replace #"-(\w)" match->upper-case)
                    (str/replace #"_(\w)" match->upper-case)
                    (keyword))]
    [new-key v]))

(defn ->camel-case
  "Take map `m` with kebab or snake case keys and produce new one with snake case."
  [m]
  (apply hash-map (mapcat ->camel-key m)))

(defn remove-nils
  [m]
  (apply dissoc m (for [[k v] m :when (nil? v)] k)))
