(ns ring-learn.http.restructure
  (:require [buddy.auth.accessrules :refer [restrict]]
            [compojure.api.meta :refer [restructure-param]]
            [ring.util.http-response :refer [unauthorized]]))

(defn access-error
  "Raise unauthorized error."
  [handler val]
  (unauthorized {:message "You are not authorized!"}))

(defn wrap-restricted
  [handler rule]
  (restrict handler {:handler rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))
