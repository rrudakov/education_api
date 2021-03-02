(ns education.http.endpoints.users
  (:require [buddy.sign.jwt :as jwt]
            [compojure.api.core :refer [context DELETE GET PATCH POST routes]]
            [education.config :as config]
            [education.database.users :as usersdb]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.common :as spec]
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
      (status/not-found {:message const/not-found-error-message})
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
  (routes
   (POST "/login" []
     :tags ["users"]
     :body [credentials ::specs/login-request]
     :summary "Authorize user using provided credentials"
     :responses {200 {:description "Success!"
                      :schema      ::specs/token-response}
                 400 {:description const/bad-request-error-message
                      :schema      ::spec/error-response}
                 401 {:description "Invalid username or password!"
                      :schema      ::spec/error-response}}
     (login-handler db config credentials))
   (context "/users" []
     :tags ["users"]
     (GET "/" []
       :middleware [[require-roles #{:moderator}]]
       :summary "Get all users"
       :description "Return the entire list of users from database"
       :responses {200 {:description "Successful"
                        :schema      ::specs/users-response}
                   401 {:description const/no-access-error-message
                        :schema      ::spec/error-response}
                   403 {:description const/not-authorized-error-message
                        :schema      ::spec/error-response}}
       (all-users-handler db))
     (GET "/:id" []
       :middleware [[require-roles #{:moderator}]]
       :path-params [id :- ::specs/id]
       :summary "Fetch user from database by ID"
       :responses {200 {:description "Successful"
                        :schema      ::specs/user-response}
                   401 {:description "Access denied!"
                        :schema      ::spec/error-response}
                   404 {:description "User not found!"
                        :schema      ::spec/error-response}}
       (get-user-handler db id))
     (POST "/" []
       :body [user ::specs/user-create-request]
       :summary "Register new user"
       :responses {200 {:description "Successful"
                        :schema      ::specs/user-create-response}
                   400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}
                   409 {:description const/conflict-error-message
                        :schema      ::spec/error-response}}
       (add-user-handler db user))
     (PATCH "/:id" []
       :middleware [[require-roles #{:admin}]]
       :body [user ::specs/user-update-request]
       :path-params [id :- ::specs/id]
       :summary "Update user"
       :description "Update user (only roles updating is supported now)"
       :responses {401 {:description const/no-access-error-message
                        :schema      ::spec/error-response}}
       (update-user-handler db id user))
     (DELETE "/:id" []
       :middleware [[require-roles #{:admin}]]
       :path-params [id :- ::specs/id]
       :summary "Delete user by ID"
       :responses {401 {:description const/no-access-error-message
                        :schema      ::spec/error-response}}
       (delete-user-handler db id)))))
