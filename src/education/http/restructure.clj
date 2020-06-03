(ns education.http.restructure
  (:require [buddy.auth :refer [authenticated?]]
            [clojure.set :as set]
            [compojure.api.meta :refer [restructure-param]]
            [education.http.constants :refer :all]
            [ring.util.http-response :refer [forbidden unauthorized]]))

(defn- expire?
  "Check if `user` data from token is expired."
  [user]
  (let [now (quot (System/currentTimeMillis) 1000)]
    (if (> now (:exp user))
      user
      nil)))

(defn- has-role?
  "Check if `user` has one or many of `required-roles`."
  [user required-roles]
  (let [user-roles (->> user :roles (map keyword))
        has-roles (cond
                    (some #{:admin} user-roles)     #{:any :guest :moderator :admin}
                    (some #{:moderator} user-roles) #{:any :guest :moderator}
                    (some #{:guest} user-roles)     #{:any :guest}
                    :else #{})
        matched-roles (set/intersection has-roles required-roles)]
    (not (empty? matched-roles))))

(defn require-roles
  "Closure to check access to particular endpoint."
  [handler roles]
  (fn [request]
    (if-not (authenticated? request)
      (unauthorized {:message not-authorized-error-message})
      (let [identity (:identity request)]
        (if-not (has-role? (:user identity) roles)
          (forbidden {:message no-access-error-message})
          (handler request))))))

;; Let's keep it just as an example of multimethod
;; (defmethod restructure-param :auth-roles
;;   [_ required-roles acc]
;;   (update-in acc [:middleware] conj [require-roles required-roles]))
