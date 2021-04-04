(ns hub.admin
  (:require
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.crux :as flux]
    [buddy.core.nonce :as nonce]))

(defn sys []
  (let [{:keys [flub.crux/node] :as sys} @flub/system]
    (assoc sys :flub.crux/db (delay (crux/db node)))))

(comment
  (flub/base64-encode (nonce/random-bytes 16))

  (let [{:keys [flub.crux/db]} (sys)]
    (flux/q-entity @db [[:user/email]])
    )
  )
