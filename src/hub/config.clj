(ns hub.config
  (:require
    [flub.core :as flub]))

(def env-keys
  ; env key | clj key | coerce fn
  [["MAILGUN_KEY"       :mailgun/secret]
   ["MAILGUN_ENDPOINT"  :mailgun/endpoint]
   ["MAILGUN_FROM"      :mailgun/from]
   ["MAILGUN_MOCK"      :mailgun/mock #(= "true" %)]
   ["JWT_KEY"           :jwt/secret]
   ["COOKIE_KEY"        :cookie/secret]
   ["BASE_URL"          :hub/base-url]
   ["MIDDLEWARE_SECURE" :hub.middleware/secure #(= "true" %)]
   ["CRUX_TOPOLOGY"     :flub.crux/topology keyword]
   ["CRUX_DIR"          :flub.crux/dir]
   ["PORT"              :flub.web/port #(Long/parseLong %)]])

(defn merge-config [sys]
  (merge
    sys
    {:flub.jetty/quiet true
     :flub.web/port 8080
     :mailgun/endpoint "https://api.mailgun.net/v3/mail.findka.com/messages"
     :mailgun/from "Hubert <contact@mail.findka.com>"}
    (flub/read-env env-keys)))
