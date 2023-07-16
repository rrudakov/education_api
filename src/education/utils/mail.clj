(ns education.utils.mail
  (:require
   [education.config :as config]
   [hato.client :as hato]
   [clojure.data.json :as json]))

(defn- free-video-mail-request-body
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
  [http-client conf token to]
  (let [base-url   (config/send-grid-base-url conf)
        url        (str base-url "/mail/send")
        api-key    (config/send-grid-api-key conf)
        auth-token (str "Bearer " api-key)]
    (hato/post url
               {:http-client  http-client
                :body         (json/write-str (free-video-mail-request-body conf token to))
                :headers      {"Authorization" auth-token}
                :content-type :json})))
