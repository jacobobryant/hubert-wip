(ns hub.util
  (:require
    [flub.core :as flub]
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

(defn emptyish? [x]
  "Like empty?, but return false whenever empty? would throw an exception."
  (boolean (flub/catchall (empty? x))))

(defn assoc-not-empty [m & kvs]
  (apply flub/assoc-pred m (complement emptyish?) kvs))

(defn dissoc-empty [m]
  (apply dissoc m (filter #(emptyish? (m %)) (keys m))))
