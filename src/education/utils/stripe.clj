(ns education.utils.stripe
  (:require [education.config :as config])
  (:import com.stripe.model.checkout.Session
           [com.stripe.param.checkout SessionCreateParams SessionCreateParams$LineItem SessionCreateParams$LineItem$PriceData SessionCreateParams$LineItem$PriceData$ProductData SessionCreateParams$Mode SessionCreateParams$PaymentMethodType]
           com.stripe.Stripe))

(defn create-checkout-session
  [config product-name price]
  (set! Stripe/apiKey (config/stripe-api-key config))
  (let [success-url  (config/payment-success-url config)
        fail-url     (config/payment-fail-url config)
        product-data (-> (SessionCreateParams$LineItem$PriceData$ProductData/builder)
                         (.setName product-name)
                         (.build))
        price-data   (-> (SessionCreateParams$LineItem$PriceData/builder)
                         (.setCurrency "rub")
                         (.setUnitAmount (long (* price 100)))
                         (.setProductData product-data)
                         (.build))
        line-item    (-> (SessionCreateParams$LineItem/builder)
                         (.setQuantity 1)
                         (.setPriceData price-data)
                         (.build))
        params       (-> (SessionCreateParams/builder)
                         (.addPaymentMethodType SessionCreateParams$PaymentMethodType/CARD)
                         (.setMode SessionCreateParams$Mode/PAYMENT)
                         (.setSuccessUrl success-url)
                         (.setCancelUrl fail-url)
                         (.addLineItem line-item)
                         (.build))
        session      (Session/create params)]
    (.getId session)))
