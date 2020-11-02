(ns education.utils.maps)

(defn unqualify-keyword
  "Take pair of qualified key and value and produce map entry with unqualified
  keys."
  [[k v]]
  [(keyword (name k)) v])

(defn unqualify-map
  "Take map `m` with qualified keys and produce new one with unqualified keys."
  [m]
  (apply hash-map (mapcat unqualify-keyword m)))
