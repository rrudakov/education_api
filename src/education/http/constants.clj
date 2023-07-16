(ns education.http.constants
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [phrase.alpha :as p]))

(def not-found-error-message "Resource not found")
(def conflict-error-message "Resource already exist")
(def bad-request-error-message "Please check request data")
(def not-authorized-error-message "You are not authorized")
(def no-access-error-message "You don't have access to this resource")
(def server-error-message "Something went wrong! Please, be patient, we're working on fix!")
(def invalid-credentials-error-message "Invalid username or password")

(def valid-url-regex
  "Regex to check video and image URLs."
  #"((http|https)://)(www.)*[a-zA-Z0-9@:%._\+~#?&//=]{2,256}\b([-a-zA-Z0-9@:%._\+~#?&//=]*)")

(def valid-decimal
  "Regex to check price."
  #"\d+(\.\d+)?")

(def valid-username-regex
  "Check new username using this regex."
  #"^[a-zA-Z]+[a-zA-Z0-9]*$")

(def valid-email-regex
  "Check new email addresses using this regex."
  #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defn- ->prefix-or
  [{:keys [in via]} or-value]
  (->> (or (first in) (first via) or-value)
       (name)
       (str/capitalize)))

;; Phrases
(p/defphraser #(contains? % key)
  [_ _ key]
  (format "Field %s is mandatory" (name key)))

(p/defphraser #(re-matches valid-username-regex %)
  [_ _]
  "Username must start from letter")

(p/defphraser #(>= (count %) min-length)
  [_ problem min-length]
  (str (->prefix-or problem "value") " must be at least " min-length " characters"))

(p/defphraser (complement str/blank?)
  [_ problem]
  (str (->prefix-or problem "value") " must not be empty"))

(p/defphraser #(<= (count %) max-length)
  [_ problem max-length]
  (str (->prefix-or problem "value") " must not be longer than " max-length " characters"))

(p/defphraser (partial re-matches valid-url-regex)
  [_ problem]
  (str (->prefix-or problem "field") " URL is not valid"))

(p/defphraser (partial re-matches valid-decimal)
  [_ problem]
  (str (->prefix-or problem "value") " is not valid"))

(p/defphraser #(re-matches valid-email-regex %)
  [_ _]
  "Email is not valid")

(p/defphraser :default
  [_ problem]
  (str (->prefix-or problem "value") " is not valid"))

;; Convert problems to
(defn ->phrases
  "Given a spec and a value x, phrases the first problem using context if any.

  Returns nil if x is valid or no phraser was found. See phrase for details.
  Use phrase directly if you want to phrase more than one problem."
  [spec x]
  (some->> (s/explain-data spec x)
           ::s/problems
           (into []
                 (comp
                  (map #(p/phrase {} %))
                  (distinct)
                  (filter some?)))))
