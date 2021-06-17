(ns education.http.endpoints.payments
  (:require [compojure.api.core :refer [context POST]]
            [ring.util.http-response :as status]
            [education.database.orders :as orders-db]
            [education.database.materials-orders :as materials-orders-db]
            [education.config :as config])
  (:import com.google.gson.JsonSyntaxException
           com.stripe.exception.SignatureVerificationException
           com.stripe.net.Webhook))

(defn- create-event
  [payload sig-header endpoint-secret]
  (try
    (Webhook/constructEvent payload sig-header endpoint-secret)
    (catch JsonSyntaxException e
      (println (.getMessage e))
      (status/bad-request {:message "Invalid payload"}))
    (catch SignatureVerificationException sve
      (println (.getMessage sve))
      (status/bad-request {:message "Invalid signature"}))
    (catch Exception e
      (println (.getMessage e))
      (status/bad-request {:message "Server error"}))))

(defn- fulfill-materials-order
  [conn session-id email]
  (let [material-id (materials-orders-db/get-by-session-id conn session-id)]
    (println "Send material with ID: " material-id)
    (orders-db/update-order conn session-id email true)))

(defn- webhook-event-handler
  [conn config]
  (fn [{:keys [headers body]}]
    (let [sig-header               (get headers "stripe-signature")
          endpoint-secret          (config/stripe-webhook-secret config)
          raw-payload              (slurp (.bytes body))
          event                    (create-event raw-payload sig-header endpoint-secret)
          data-object-deserializer (.getDataObjectDeserializer event)]
      (when (and (= "checkout.session.completed" (. event getType))
               (.. data-object-deserializer getObject isPresent))
        (let [session    (.. data-object-deserializer getObject get)
              session-id (. session getId)
              email      (.. session getCustomerDetails getEmail)
              order      (orders-db/get-by-session-id conn session-id)]
          (case (-> order :orders/product_type keyword)
            :materials (fulfill-materials-order conn session-id email)
            (println "Unknown product type"))))
      (status/ok))))

(defn payments-routes
  "Define routes for payments API."
  [conn config]
  (context "/payments" []
    :tags ["payments"]
    (POST "/webhook" []
      :summary "Webhook for Stripe payments"
      (webhook-event-handler conn config))))
