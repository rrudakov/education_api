(ns education.http.endpoints.users
  (:require [buddy.sign.jwt :as jwt]
            [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [education.config :as config]
            [education.database.users :as usersdb]
            [education.http.restructure :refer [require-roles]]
            [education.specs.users :as specs]
            [ring.swagger.schema :refer [describe]]
            [ring.util.http-response
             :refer
             [created no-content not-found ok unauthorized]]))

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
        exp (.plusSeconds (java.time.Instant/now) (* 60 60 24))]
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

(defn- update-user-handler
  "Update existing user by `user-id`."
  [db user-id user]
  (do
    (usersdb/update-user db user-id user)
    (no-content)))

(defn- delete-user-handler
  "Delete existing user handler."
  [db user-id]
  (do
    (usersdb/delete-user db user-id)
    (no-content)))

;; Define routes
(defn users-routes
  "Define routes for users endpoint."
  [db config]
  (context "" []
   :tags ["users"]
   (GET "/users" []
     :middleware [[require-roles #{:moderator}]]
     :return ::specs/user-response      ;List
     :summary "Return the entire list of users from database"
     (all-users-handler db))
   (GET "/users/:id" []
     :middleware [[require-roles #{:moderator}]]
     :path-params [id :- (describe ::specs/id "Specify user ID")]
     :return ::specs/user-response
     :summary "Fetch user from database by ID"
     (get-user-handler db id))
   (POST "/users" []
     :body [user ::specs/user-create-request]
     :return (describe ::specs/id "New ID for created user")
     :summary "Register new user"
     (add-user-handler db user))
   (PATCH "/users/:id" []
     :middleware [[require-roles #{:admin}]]
     :body [user ::specs/user-update-request]
     :return {}
     :path-params [id :- (describe ::specs/id "Specify user ID")]
     :summary "Update user (only roles updating is supported now)"
     (update-user-handler db id user))
   (DELETE "/users/:id" []
     :middleware [[require-roles #{:admin}]]
     :return {}
     :path-params [id :- (describe ::specs/id "Specify user ID")]
     :summary "Delete user by ID"
     (delete-user-handler db id))
   (POST "/login" []
     :body [credentials ::specs/login-request]
     :return (describe ::specs/token-response "JWT token for following authorized requests")
     :summary "Authorize user using provided credentials"
     (login-handler db config credentials))))
