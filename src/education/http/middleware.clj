(ns education.http.middleware
  (:require
   [buddy.auth :refer [authenticated?]]
   [clojure.set :as set]
   [education.http.constants :as const]
   [ring.util.http-response :as status]))

(defn- has-role?
  "Check if `user` has one or many of `required-roles`."
  [user required-roles]
  (let [user-roles    (->> user :roles (map keyword) set)
        has-roles     (cond
                        (contains? user-roles :admin)     #{:any :guest :moderator :admin}
                        (contains? user-roles :moderator) #{:any :guest :moderator}
                        (contains? user-roles :guest)     #{:any :guest}
                        :else                             #{})
        matched-roles (set/intersection has-roles required-roles)]
    (boolean (seq matched-roles))))

(def require-roles
  {:name    ::require-roles
   :compile (fn [_ _]
              (fn [handler roles]
                (fn [request]
                  (if-not (authenticated? request)
                    (status/unauthorized {:message const/not-authorized-error-message})
                    (let [idt (:identity request)]
                      (if-not (has-role? (:user idt) roles)
                        (status/forbidden {:message const/no-access-error-message})
                        (handler request)))))))})
