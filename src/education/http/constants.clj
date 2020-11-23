(ns education.http.constants)

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
