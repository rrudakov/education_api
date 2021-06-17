(ns education.database.orders
  (:require [next.jdbc.sql :as sql]
            [cljc.java-time.instant :as instant]))

(defn create-order
  [conn session-id product-type]
  (->> {:session_id   session-id
        :product_type (name product-type)}
       (sql/insert! conn :orders)
       (:orders/session_id)))

(defn update-order
  [conn session-id email fulfilled?]
  (sql/update! conn :orders
               {:email      email
                :fulfilled  fulfilled?
                :updated_on (instant/now)}
               {:session_id session-id}))

(defn get-by-session-id
  [conn session-id]
  (sql/get-by-id conn :orders session-id :session_id {}))
