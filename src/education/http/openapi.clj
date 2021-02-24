(ns education.http.openapi
  (:require [clojure.string :as str]
            [compojure.api.middleware :as mw]
            [compojure.api.request :as request]
            [compojure.api.sweet :refer [routes undocumented GET]]
            [ring.swagger.common :as rsc]
            [ring.swagger.core :as swagger]
            [ring.swagger.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer [ok]]
            [spec-tools.openapi.core :as openapi]))

(defn base-path
  [request]
  (let [context (swagger/context request)]
    (if (= "" context) "/" context)))

(defn- openapi-path
  [uri]
  (str/replace uri #":([\p{L}_][\p{L}_0-9-]*)" "{$1}"))

(defn swagger-ui
  [options]
  (undocumented
   (swagger-ui/swagger-ui options)))

(def openapi-defaults
  {:openapi "3.0.3"
   :info
   {:title   "OpenAPI"
    :version "0.0.1"}})

(defn request-body->contents
  [parameters consumes]
  (if-let [contents (get-in parameters [:requestBody ::openapi/content])]
    (let [new-contents (into (empty contents)
                             (map (fn [content-type]
                                    [content-type (get contents "default")]) consumes))]
      (assoc-in parameters [:requestBody ::openapi/content] new-contents))
    parameters))

(defn responses->contents
  [parameters produces]
  (let [responses (:responses parameters)]
    (assoc parameters
           :responses
           (into (empty responses)
                 (map (fn [[code response]]
                        (if-let [content (::openapi/content response)]
                          [code {::openapi/content
                                 (into (empty content)
                                       (map (fn [content-type]
                                              [content-type (get content "default")]) produces))}]
                          [code response]))
                      responses)))))

(defn remove-parameters
  [p {:keys [consumes produces]}]
  (into (empty p)
        (map (fn [[path methods]]
               [(openapi-path path) (into (empty methods)
                                          (map (fn [[method v]]
                                                 [method (-> (dissoc v :parameters)
                                                             (request-body->contents consumes)
                                                             (responses->contents produces))]) methods))]) p)))

(defn openapi-docs
  [{:keys [path] :or {path "/openapi.json"} :as options}]
  (let [extra-info (dissoc options :path)]
    (GET path request
      :no-doc true
      :name ::swagger
      (let [runtime-info1 (mw/get-swagger-data request)
            base-path     {:basePath (base-path request)}
            options       (::request/ring-swagger request)
            paths         (update (::request/paths request) :paths (fn [path] (remove-parameters path runtime-info1)))
            openapi       (merge openapi-defaults
                                 (apply rsc/deep-merge
                                        (keep identity [base-path paths extra-info runtime-info1])))
            spec          (openapi/openapi-spec openapi options)]
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
