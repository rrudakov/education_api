(ns education.http.endpoints.users
  (:require
   [buddy.sign.jwt :as jwt]
   [education.config :as config]
   [education.database.users :as usersdb]
   [education.http.constants :as const]
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
(defn login-handler
  "Handle login request."
  [{:keys [conn app-config] {:keys [body]} :parameters}]
  (let [[ok? res] (create-auth-token conn app-config body)]
    (if ok?
      (status/ok res)
      (status/unauthorized res))))

(defn all-users-handler
  "Return all users list."
  [{:keys [conn]}]
  (->> conn
       usersdb/get-all-users
       (mapv to-user-response)
       status/ok))

(defn get-user-handler
  "Get user by ID handler."
  [{:keys [conn] {{:keys [user-id]} :path} :parameters}]
  (let [user (usersdb/get-user conn user-id)]
    (if (nil? user)
      (status/not-found {:message const/not-found-error-message})
      (status/ok (to-user-response user)))))

(defn add-user-handler
  "Create new user handler."
  [{:keys [conn] {:keys [body]} :parameters}]
  (status/created (str (usersdb/add-user conn body))))

(defn update-user-handler
  "Update existing user by `user-id`."
  [{:keys [conn] {:keys [body] {:keys [user-id]} :path} :parameters}]
  (usersdb/update-user conn user-id body)
  (status/no-content))

(defn delete-user-handler
  "Delete existing user handler."
  [{:keys [conn] {{:keys [user-id]} :path} :parameters}]
  (usersdb/delete-user conn user-id)
  (status/no-content))
