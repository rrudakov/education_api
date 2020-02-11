(ns ring-learn.http.component
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as server]
            [ring-learn.database.users :refer :all]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]))

(s/defschema UserCreateRequest
  {:user_name s/Str
   :user_password s/Str
   :user_email s/Str
   :is_admin s/Bool})

(s/defschema User
  {:id s/Int
   :user_name s/Str
   :user_password s/Str
   :user_email s/Str
   :is_admin s/Bool
   :created_on s/Inst
   :updated_on s/Inst})

(defn create
  [db]
  (api
   {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "Dice-api"
                    :description "Compojure Api example"}
             :tags [{:name "api", :description "some apis"}]}}}

    (context "/api" []
      :tags ["api"]

      (GET "/plus" []
        :return {:result Long}
        :query-params [x :- Long, y :- Long]
        :summary "adds two numbers together"
        (ok {:result (+ x y)}))
      (GET "/users" []
        :return [User]
        :summary "Return the entire list of users from database"
        (ok (get-all-users db)))
      (POST "/users" []
        :body [user UserCreateRequest]
        :return s/Int
        :summary "Create new user"
        (ok (add-user db user))))))

(defrecord WebServer [srv db]
  component/Lifecycle

  (start [component]
    (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
      (println (str ";; Running web server at http://127.0.0.1:" port "/"))
      (assoc component :srv
             (server/run-server (wrap-defaults (create db) api-defaults) {:port port}))))

  (stop [component]
    (println ";; Stopping web server")
    (srv :timeout 100)
    (assoc component :srv nil)))

(defn new-webserver
  []
  (component/using (map->WebServer {}) [:db]))
