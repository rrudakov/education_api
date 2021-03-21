(ns education.utils.mail-test
  (:require [education.utils.mail :as sut]
            [clojure.test :refer [deftest testing is]]
            [clj-http.client :as client]
            [spy.core :as spy]
            [education.test-data :as td]
            [education.config :as config]))

(deftest send-free-lesson-email-http-test
  (testing "Test send free lesson using sendgrid successfully"
    (with-redefs [client/post (spy/spy)]
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
        (sut/send-free-lesson-email-http td/test-config token to)
        (is (spy/called-once-with? client/post
                                   url-expected
                                   {:form-params  form-params-expected
                                    :headers      {"Authorization" auth-token-expected}
                                    :content-type :json}))))))
