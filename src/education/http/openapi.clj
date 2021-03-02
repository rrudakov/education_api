(ns education.http.openapi
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [compojure.api.middleware :as mw]
            [compojure.api.request :as request]
            [compojure.api.sweet :refer [GET routes undocumented]]
            [ring.swagger.common :as rsc]
            [ring.swagger.core :as swagger]
            [ring.swagger.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer [ok]]
            [spec-tools.openapi.core :as openapi]))

(defn update-parameters
  [x]
  (walk/postwalk
   (fn [y]
     (if (and (map-entry? y) (= (first y) :parameters))
       nil
       y))
   x))

(defn replace-content
  [x content-types]
  (walk/postwalk
   (fn [y]
     (if (and (map-entry? y) (= (first y) ::openapi/content))
       (if-let [body (get (second y) "default")]
         [::openapi/content
          (into {} (map (fn [content-type]
                          [content-type body])
                        content-types))]
         y)
       y))
   x))

(defn update-request-bodies
  [x consumes]
  (walk/postwalk
   (fn [y]
     (if (map-entry? y)
       (if (= (first y) :requestBody)
         (replace-content y consumes)
         y)
       y))
   x))

(defn update-responses
  [x produces]
  (walk/postwalk
   (fn [y]
     (if (map-entry? y)
       (if (= (first y) :responses)
         (replace-content y produces)
         y)
       y))
   x))

(defn base-path
  [request]
  (let [context (swagger/context request)]
    (if (= "" context) "/" context)))

(defn- openapi-path
  [uri]
  (str/replace uri #":([\p{L}_][\p{L}_0-9-]*)" "{$1}"))

(defn update-paths
  [x]
  (let [paths (:paths x)]
    (assoc x :paths
           (into (empty paths)
                 (map (fn [[path methods]]
                        [(openapi-path path) methods]) paths)))))

(defn swagger-ui
  [options]
  (undocumented
   (swagger-ui/swagger-ui options)))

(def openapi-defaults
  {:openapi "3.0.3"
   :info
   {:title   "OpenAPI"
    :version "0.0.1"}})

(defn openapi-docs
  [{:keys [path] :or {path "/openapi.json"} :as options}]
  (let [extra-info (dissoc options :path)]
    (GET path request
      :no-doc true
      :name ::swagger
      (let [runtime-info (mw/get-swagger-data request)
            base-path    {:basePath (base-path request)}
            options      (::request/ring-swagger request)
            paths        (-> (::request/paths request)
                             (update-parameters)
                             (update-request-bodies (:consumes runtime-info))
                             (update-responses (:produces runtime-info))
                             (update-paths))
            openapi      (merge openapi-defaults
                                (apply rsc/deep-merge
                                       (keep identity [base-path paths extra-info])))
            spec         (openapi/openapi-spec openapi options)]
        (ok spec)))))

(def openapi-default-options
  {:ui   "/"
   :spec "/openapi.json"})

(defn openapi-routes
  ([]
   (openapi-routes {}))
  ([options]
   (let [{:keys [ui spec data] {ui-options :ui} :options} (merge openapi-default-options options)
         path                                             (apply str (remove str/blank? [(:basePath data) spec]))]
     (when (or ui spec)
       (routes
        (when ui (swagger-ui (merge (when spec {:swagger-docs path}) ui-options {:path ui})))
        (when spec (openapi-docs (assoc data :path spec))))))))
