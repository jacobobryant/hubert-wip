(ns hub.util
  (:require
    [ring.middleware.anti-forgery :as anti-forgery]))

(defn email= [s1 s2]
  (.equalsIgnoreCase s1 s2))

(defn wrap-auth-required [handler]
  (anti-forgery/wrap-anti-forgery
    (fn [{:keys [session/uid] :as req}]
      (if uid
        (handler req)
        {:status 302
         :headers/Location "/?error=unauthenticated"}))))
