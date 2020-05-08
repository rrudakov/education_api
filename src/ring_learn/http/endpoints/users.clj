(ns ring-learn.http.endpoints.users
  (:require [compojure.api.sweet :refer [context GET POST]]
            [ring-learn.database.users :as usersdb]
            [ring-learn.http.restructure :refer [require-roles]]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response :refer [created not-found ok unauthorized]]
            [schema.core :as s]
            [clj-time.core :as t]
            [buddy.sign.jwt :as jwt]
            [ring-learn.config :as config]))

;; Model definitions
(s/defschema UserCreateRequest
  {:username s/Str
   :password s/Str
   :email s/Str})

(s/defschema User
  {:id s/Int
   :username s/Str
   :email s/Str
   :roles #{s/Keyword}
   :created_on s/Inst
   :updated_on s/Inst})

(s/defschema LoginRequest
  {:username s/Str
   :password s/Str})

(s/defschema Token
  {:token s/Str})

;; Converters
(defn to-user-response
  "Convert database user to user response."
  [{:users/keys [id user_name user_email created_on updated_on roles]}]
  {:id id
   :username user_name
   :email user_email
   :roles roles
   :created_on created_on
   :updated_on updated_on})

;; Helpers
(defn create-auth-token
  "Create authorization token based on `credentials`."
  [db config credentials]
  (let [[ok? res] (usersdb/auth-user db credentials)
        exp (t/plus (t/now) (t/days 1))]
    (if ok?
      [true {:token (-> res
                        (update-in [:user] to-user-response)
                        (assoc :exp exp)
                        (jwt/sign (config/token-sign-secret config) {:alg :hs512}))}]
      [false res])))

;; Handlers
(defn- login-handler
  "Handle login request."
  [db config credentials]
  (let [[ok? res] (create-auth-token db config credentials)]
    (if ok?
      (ok res)
      (unauthorized res))))

(defn- all-users-handler
  "Return all users list."
  [db]
  (->> db
       usersdb/get-all-users
       (map to-user-response)
       ok))

(defn- get-user-handler
  "Get user by ID handler."
  [db id]
  (let [user (usersdb/get-user db id)]
    (if (nil? user)
      (not-found {:message (str "User with id " id " not found!")})
      (ok (to-user-response user)))))

(defn- add-user-handler
  "Create new user handler."
  [db user]
  (created (str (usersdb/add-user db user))))

;; Define routes
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
     :middleware [[require-roles #{:moderator}]]
     :path-params [id :- (describe s/Int "Specify user ID")]
     :return User
     :summary "Fetch user from database by ID"
     (get-user-handler db id))
   (POST "/users" []
     :body [user UserCreateRequest]
     :return (describe s/Int "New ID for created user")
     :summary "Create new user"
     (add-user-handler db user))
   (POST "/login" []
     :body [credentials LoginRequest]
     :return (describe Token "JWT token for following authorized requests")
     :summary "Authorize user using provided credentials"
     (login-handler db config credentials))))
