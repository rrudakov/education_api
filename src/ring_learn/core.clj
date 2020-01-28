(ns ring-learn.core
  (:require [cheshire.core :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer :all]))

;; Define server variable
(defonce srv (atom nil))

;; Define in memory entities
(def people-collection (atom []))

;; Helper functions
(defn add-person
  "Add person to `people-collection` vector."
  [firstname surname]
  (swap! people-collection conj
         {:firstname (str/capitalize firstname)
          :surname (str/capitalize surname)}))

(defn- parse-json-request
  "Parse JSON request and return map."
  [request]
  (parse-stream (clojure.java.io/reader (:body request)) true))

(defn- json-response
  "Encode object as JSON string with pretty print."
  [obj]
  (generate-string obj {:pretty true}))

;; Handlers
(defn simple-body-page
  "Default handler, which just returns hello world."
  [request]
  (content-type
   (response "Hello World!")
   "text/html"))

(defn request-example
  "Return all request details."
  [request]
  (content-type
   (response (str "Request object: " request))
   "text/html"))

(defn hello-name
  "Example of params handling."
  [request]
  (let [name (:name (:params request))]
    (content-type
     (response (str "Hello " name))
     "text/html")))

(defn people-handler
  "Return list of people from `people-collection` vector."
  [request]
  (content-type
   (response (json-response @people-collection))
   "application-json"))

(defn add-person-handler
  "Add person to `person-collection` vector."
  [request]
  (let [body (parse-json-request request)]
    (add-person (:firstname body) (:lastname body))
    (content-type
     (created (str (count @people-collection)))
     "application/json")))


;; Server staff
(defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/request" [] request-example)
  (GET "/hello" [] hello-name)
  (GET "/people" [] people-handler)
  (POST "/people/add" [] add-person-handler)
  (route/not-found "Error, page not found!"))

(defn stop-server
  "Stop current running server."
  []
  (when-not (nil? @srv)
    (@srv :timeout 100)
    (reset! srv nil)))

(defn -main
  "Run HTTP server."
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (reset! srv (server/run-server (wrap-defaults #'app-routes api-defaults) {:port port}))
    (println (str "Running webserver at http://127.0.0.1:" port "/"))))
