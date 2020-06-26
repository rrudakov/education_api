(ns education.http.endpoints.users
  (:require [buddy.sign.jwt :as jwt]
            [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [education.config :as config]
            [education.database.users :as usersdb]
            [education.http.constants :refer [not-found-error-message]]
            [education.http.restructure :refer [require-roles]]
            [education.specs.error :as err]
            [education.specs.users :as specs]
            [ring.util.http-response :as status]))

;; Converters
(defn to-user-response
  "Convert database user to user response."
  [{:users/keys [id user_name user_email created_on updated_on roles]}]
  {:id         id
   :username   user_name
   :email      user_email
   :roles      roles
   :created_on created_on
   :updated_on updated_on})

;; Helpers
(defn create-auth-token
  "Create authorization token based on `credentials`."
  [db config credentials]
  (let [[ok? res] (usersdb/auth-user db credentials)
        exp       (.plusSeconds (java.time.Instant/now) (* 60 60 24))]
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
      (status/ok res)
      (status/unauthorized res))))

(defn- all-users-handler
  "Return all users list."
  [db]
  (->> db
       usersdb/get-all-users
       (map to-user-response)
       vec
       status/ok))

(defn- get-user-handler
  "Get user by ID handler."
  [db id]
  (let [user (usersdb/get-user db id)]
    (if (nil? user)
      (status/not-found {:message not-found-error-message})
      (status/ok (to-user-response user)))))

(defn- add-user-handler
  "Create new user handler."
  [db user]
  (status/created (str (usersdb/add-user db user))))

(defn- update-user-handler
  "Update existing user by `user-id`."
  [db user-id user]
  (usersdb/update-user db user-id user)
  (status/no-content))

(defn- delete-user-handler
  "Delete existing user handler."
  [db user-id]
  (usersdb/delete-user db user-id)
  (status/no-content))

;; Define routes
(defn users-routes
  "Define routes for users endpoint."
  [db config]
  (context "" []
    :tags ["users"]
    (GET "/users" []
      :middleware [[require-roles #{:moderator}]]
      :return ::specs/users-response
      :summary "Return the entire list of users from database"
      :responses {401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (all-users-handler db))
    (GET "/users/:id" []
      :middleware [[require-roles #{:moderator}]]
      :path-params [id :- ::specs/id]
      :return ::specs/user-response
      :summary "Fetch user from database by ID"
      :responses {404 {:description "User not found!"
                       :schema      ::err/error-response}
                  401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (get-user-handler db id))
    (POST "/users" []
      :body [user ::specs/user-create-request]
      :return ::specs/id
      :summary "Register new user"
      :responses {409 {:description "User already exist!"
                       :schema      ::err/error-response}
                  400 {:description "Invalid request body!"
                       :schema      ::err/error-response}}
      (add-user-handler db user))
    (PATCH "/users/:id" []
      :middleware [[require-roles #{:admin}]]
      :body [user ::specs/user-update-request]
      :return {}
      :path-params [id :- ::specs/id]
      :summary "Update user (only roles updating is supported now)"
      :responses {401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (update-user-handler db id user))
    (DELETE "/users/:id" []
      :middleware [[require-roles #{:admin}]]
      :return {}
      :path-params [id :- ::specs/id]
      :summary "Delete user by ID"
      :responses {401 {:description "Access denied!"
                       :schema      ::err/error-response}}
      (delete-user-handler db id))
    (POST "/login" []
      :body [credentials ::specs/login-request]
      :return ::specs/token-response
      :summary "Authorize user using provided credentials"
      :responses {200 {:description "Success!"
                       :schema      ::specs/token-response}
                  400 {:description "Invalid request body!"
                       :schema      ::err/error-response}
                  401 {:description "Invalid username or password!"
                       :schema      ::err/error-response}}
      (login-handler db config credentials))))
