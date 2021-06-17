(ns education.database.materials-orders
  (:require [next.jdbc.sql :as sql]))

(defn create-material-order
  [conn session-id material-id]
  (->> {:order_session_id  session-id
        :material_id material-id}
       (sql/insert! conn :materials_orders)
       (:materials_orders/order_session_id)))

(defn get-by-session-id
  [conn session-id]
  (sql/get-by-id conn :materials_orders session-id :order_session_id {}))
