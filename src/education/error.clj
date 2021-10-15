(ns education.error
  (:require
   [com.brunobonacci.mulog :as u]
   [education.http.constants :as const]
   [reitit.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [ring.util.http-response :as status]
   [clojure.spec.alpha :as s])
  (:import
   java.sql.SQLException))

(derive ::warning ::exception)

(defn sql-exception-handler
  [^SQLException exception _]
  (u/log ::sql-exception :exception (.getMessage exception))
  (case (.getSQLState exception)
    "23505" (status/conflict {:message const/conflict-error-message})
    "23503" (status/not-found {:message const/not-found-error-message})
    "23502" (status/bad-request {:message const/bad-request-error-message})
    (status/internal-server-error {:message    (.getMessage exception)
                                   :error_code (.getSQLState exception)})))

(defn request-coercion-handler
  [e _]
  (let [{:keys [spec value]} (ex-data e)
        errors               (const/->phrases spec value)]
    (u/log ::request-body-validation-exception
           :exception (.getMessage e)
           :errors errors)
    {:status 400
     :body   {:message const/bad-request-error-message
              :errors  errors}}))

(defn response-coercion-handler
  [e _]
  (let [{:keys [spec value]} (ex-data e)]
    (u/log ::response-validation-error
           :exception (.getMessage e)
           :details (s/explain-str spec value))
    {:status 500
     :body   {:message const/server-error-message
              :details (s/explain-str spec value)}}))

(defn default-handler
  "Default safe handler for any exception."
  [^Exception e _]
  {:status 500
   :body   {:message const/server-error-message
            :details (.getMessage e)}})

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {SQLException                 sql-exception-handler
     ::coercion/request-coercion  request-coercion-handler
     ::coercion/response-coercion response-coercion-handler
     ::exception/default          default-handler})))
