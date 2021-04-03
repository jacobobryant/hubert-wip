(ns hub.admin
  (:require
    [flub.core :as flub]
    [buddy.core.nonce :as nonce]))

(comment
  (flub/base64-encode (nonce/random-bytes 16))
  )
