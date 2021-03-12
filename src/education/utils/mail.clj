(ns education.utils.mail
  (:require [clostache.parser :refer [render-resource]]
            [postal.core :refer [send-message]]))

(def smtp-creds
  {:host "smtp.gmail.com"
   :user "phentagram@gmail.com"
   :pass "guxayqtterklasce"
   :port 587
   :tls true})

(defn send-free-lesson-email
  [conf token to]
  (send-message smtp-creds
                {:from    "phentagram@gmail.com"
                 :to      to
                 :subject "Бесплатный видео-урок"
                 :body    [:alternative
                           {:type    "text/plain; charset=utf-8"
                            :content ""}
                           {:type    "text/html; charset=utf-8"
                            :content (render-resource "templates/free-lesson.mustache"
                                                      {:token token})}]}))
