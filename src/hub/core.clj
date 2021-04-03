(ns hub.core
  (:require
    [flub.core :as flub]
    [hub.components :as c])
  (:gen-class))

(defn start [first-start]
  (flub/start-system
    {:flub/first-start first-start
     :flub/after-refresh `after-refresh}
    c/all-components)
  (println "System started."))

(defn -main []
  (start true))

(defn after-refresh []
  (start false))

(comment
  (prn (keys @flub/system))
  (flub/refresh)
  )
