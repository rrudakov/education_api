(ns education.utils.mail-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [education.config :as config]
   [education.test-data :as td]
   [education.utils.mail :as sut]
   [hato.client :as hato]
   [spy.core :as spy]
   [clojure.data.json :as json]))

(deftest send-free-lesson-email-http-test
  (testing "Test send free lesson using sendgrid successfully"
    (with-redefs [hato/post (spy/spy)]
      (let [token                "some-string"
            to                   "some@email.com"
            url-expected         (str (config/send-grid-base-url td/test-config) "/mail/send")
            form-params-expected {:personalizations
                                  [{:to
                                    [{:email to}]
                                    :dynamic_template_data
                                    {:token token}
                                    :subject "Бесплатный видео-урок"}]
                                  :from
                                  {:email "info@alenkinaskazka.nl"
                                   :name  "Алёнкина сказка"}
                                  :reply_to
                                  {:email "noreply@alenkinaskazka.nl"
                                   :name  "Алёнкина сказка"}
                                  :template_id (config/free-lesson-template-id td/test-config)}
            auth-token-expected  (str "Bearer " (config/send-grid-api-key td/test-config))]
        (sut/send-free-lesson-email-http :hato-client td/test-config token to)
        (is (spy/called-once-with? hato/post
                                   url-expected
                                   {:http-client  :hato-client
                                    :body         (json/write-str form-params-expected)
                                    :headers      {"Authorization" auth-token-expected}
                                    :content-type :json}))))))
