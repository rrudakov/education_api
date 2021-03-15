(ns education.database.email-subscriptions
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn add-email-subscription
  "Create new record of `email-subscription` in database."
  [conn email]
  (jdbc/execute-one! conn
                     (-> (h/insert-into :email_subscriptions)
                         (h/values [{:email email :is_active true}])
                         (h/on-conflict :email)
                         (h/do-nothing)
                         (hsql/format))))

(defn get-email-subscription-by-email
  [conn email]
  (sql/get-by-id conn :email_subscriptions email :email {}))
