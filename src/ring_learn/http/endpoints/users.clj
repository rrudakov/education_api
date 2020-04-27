(ns ring-learn.http.endpoints.users
  (:require [compojure.api.sweet :refer [context GET POST]]
            [ring-learn.database.users :as usersdb]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response :refer [created ok unauthorized not-found]]
            [schema.core :as s]))

;; Model definitions
(s/defschema UserCreateRequest
  {:user_name s/Str
   :user_password s/Str
   :user_email s/Str})

(s/defschema User
  {:id s/Int
   :user_name s/Str
   :user_email s/Str
   :created_on s/Inst
   :updated_on s/Inst})

(s/defschema LoginRequest
  {:username s/Str
   :password s/Str})

(s/defschema Token
  {:token s/Str})

;; Handlers
(defn- login-handler
  "Handle login request."
  [db config credentials]
  (let [[ok? res] (usersdb/create-auth-token db config credentials)]
    (if ok?
      (ok res)
      (unauthorized res))))

(defn- all-users-handler
  "Return all users list."
  [db]
  (ok (usersdb/get-all-users db)))

(defn- get-user-handler
  "Get user by ID handler."
  [db id]
  (let [user (usersdb/get-user db id)]
    (if (nil? user)
      (not-found {:message (str "User with id " id " not found!")})
      (ok user))))

(defn- add-user-handler
  "Create new user handler."
  [db user]
  (created (str (usersdb/add-user db user))))

(defn users-routes
  "Define routes for users endpoint."
  [db config]
  (context "" []
   :tags ["users"]
   (GET "/users" []
     :return [User]
     :summary "Return the entire list of users from database"
     (all-users-handler db))
   (GET "/users/:id" []
     :path-params [id :- (describe s/Int "Specify user ID")]
     :return User
     :summary "Fetch user from database by ID"
     (get-user-handler db id))
   (POST "/users" []
     :body [user UserCreateRequest]
     :return (describe s/Int "New ID for created user")
     :summary "Create new user"
     (add-user-handler db user))
   (POST "/authorize" []
     :body [credentials LoginRequest]
     :return (describe Token "JWT token for following authorized requests")
     :summary "Authorize user using provided credentials"
     (login-handler db config credentials))))
