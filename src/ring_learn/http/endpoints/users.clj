(ns ring-learn.http.endpoints.users
  (:require [compojure.api.sweet :refer [GET POST routes]]
            [ring-learn.database.users :refer [add-user get-all-users get-user]]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response :refer [created ok]]
            [schema.core :as s]))

(s/defschema UserCreateRequest
  {:user_name s/Str
   :user_password s/Str
   :user_email s/Str
   :is_admin s/Bool})

(s/defschema User
  {:id s/Int
   :user_name s/Str
   :user_email s/Str
   :is_admin s/Bool
   :created_on s/Inst
   :updated_on s/Inst})

(defn users-routes
  "Define routes for users endpoint."
  [db]
  (routes
   (GET "/users" []
     :return [User]
     :summary "Return the entire list of users from database"
     (ok (get-all-users db)))
   (GET "/users/:id" []
     :path-params [id :- (describe s/Str "Specify user ID")]
     :return User
     :summary "Fetch user from database by ID"
     (ok (get-user db id)))
   (POST "/users" []
     :body [user UserCreateRequest]
     :return s/Int
     :summary "Create new user"
     (created (add-user db user)))))
