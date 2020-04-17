(ns ring-learn.http.endpoints.users
  (:require [compojure.api.sweet :refer [GET POST routes]]
            [ring-learn.database.users :as usersdb]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response :refer [created ok unauthorized]]
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

(s/defschema LoginRequest
  {:username s/Str
   :password s/Str})

(s/defschema Token
  {:token s/Str})

(defn- login-handler
  "Handle login request."
  [db config credentials]
  (let [[ok? res] (usersdb/create-auth-token db config credentials)]
    (if ok?
      (ok res)
      (unauthorized res))))

(defn users-routes
  "Define routes for users endpoint."
  [db config]
  (routes
   (GET "/users" []
     :return [User]
     :summary "Return the entire list of users from database"
     (ok (usersdb/get-all-users db)))
   (GET "/users/:id" []
     :path-params [id :- (describe s/Str "Specify user ID")]
     :return User
     :summary "Fetch user from database by ID"
     (ok (usersdb/get-user db id)))
   (POST "/users" []
     :body [user UserCreateRequest]
     :return s/Int
     :summary "Create new user"
     (created (usersdb/add-user db user)))
   (POST "/authorize" []
     :body [credentials LoginRequest]
     :return Token
     :summary "Authorize user using provided credentials"
     (login-handler db config credentials))))
