(ns education.utils.mail
  (:require [clj-http.client :as client]
            [education.config :as config]))

(defn free-video-mail-request-body
  [conf token to]
  {:personalizations
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
   :template_id (config/free-lesson-template-id conf)})

(defn send-free-lesson-email-http
  [conf token to]
  (let [base-url   (config/send-grid-base-url conf)
        url        (str base-url "/mail/send")
        api-key    (config/send-grid-api-key conf)
        auth-token (str "Bearer " api-key)]
    (client/post url
                 {:form-params  (free-video-mail-request-body conf token to)
                  :headers      {"Authorization" auth-token}
                  :content-type :json})))
