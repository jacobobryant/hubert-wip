(ns hub.extra.airtable)

(defn home [req]
  {:status 200
   :body "hello import airtable"
   :headers {"Content-Type" "text/plain"}})

(defn on-refresh []
  (println "refresh lol"))

(def plugin
  {:hub/title "Import Airtable"
   :hub/prefix "hub-airtable"
   :hub/routes {["/" :get] home}
   :hub/on-refresh on-refresh})
