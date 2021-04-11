(ns hub.admin
  (:require
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.crux :as flux]
    [buddy.core.nonce :as nonce]
    [reitit.core :as r]))

(defn sys []
  (let [{:keys [flub.crux/node] :as sys} @flub/system]
    (assoc sys :flub.crux/db (delay (crux/db node)))))

(defn generate-keys [_]
  (doseq [[var-name length] [["COOKIE_KEY" 16]
                             ["JWT_KEY" 32]]]
    (println (str var-name "="
               (flub/base64-encode (nonce/random-bytes length))))))

(comment

  (flub/pprint
    (let [{:keys [flub.crux/db ]} (sys)]
      ; test stuff here
      ))
  )
