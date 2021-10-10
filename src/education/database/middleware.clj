(ns education.database.middleware)

(def db-middleware
  {:name    ::db
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :conn db)))))})
