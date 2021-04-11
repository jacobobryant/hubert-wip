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
   ["CRUX_DIR"          :flub.crux/dir]
   ["USER_CRUX_DIR"     :hub/user-crux-dir]
   ["PORT"              :flub.web/port #(Long/parseLong %)]])

(defn merge-config [sys]
  (merge
    sys
    {:flub.crux/topology :standalone
     :flub.jetty/quiet true
     :flub.web/port 8080
     :hub.middleware/secure true}
    (flub/read-env env-keys)))
