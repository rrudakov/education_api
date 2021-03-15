(ns education.utils.crypt
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.codecs.base64 :as b64]
            [buddy.core.crypto :as crypto]
            [education.config :as config]))

(defn generate-hash
  [conf email]
  (let [k  (config/crypto-key conf)
        iv (config/crypto-iv conf)]
    (->> (-> email
             (codecs/to-bytes)
             (crypto/encrypt k iv)
             (b64/encode true))
         (map char)
         (apply str))))

(defn decrypt-hash
  [conf h]
  (let [k  (config/crypto-key conf)
        iv (config/crypto-iv conf)]
    (try
      (->> (crypto/decrypt (b64/decode h) k iv)
           (codecs/bytes->str))
      (catch Exception _
        nil))))
