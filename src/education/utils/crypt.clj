(ns education.utils.crypt
  (:require
   [buddy.core.codecs :as codecs]
   [buddy.core.crypto :as crypto]
   [education.config :as config]))

(defn generate-hash
  [conf email]
  (let [k  (config/crypto-key conf)
        iv (config/crypto-iv conf)]
    (-> email
        (codecs/to-bytes)
        (crypto/encrypt k iv)
        (codecs/bytes->b64)
        (codecs/bytes->str))))

(defn decrypt-hash
  [conf hash]
  (let [k  (config/crypto-key conf)
        iv (config/crypto-iv conf)]
    (try
      (-> hash
          (codecs/str->bytes)
          (codecs/b64->bytes)
          (crypto/decrypt k iv)
          (codecs/bytes->str))
      (catch Exception _
        nil))))
