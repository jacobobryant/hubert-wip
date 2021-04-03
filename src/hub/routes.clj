(ns hub.routes
  (:require
    [clojure.string :as str]
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.crux :as flux]
    [flub.extra :as fe]
    [flub.views :as fv]
    [hub.schema :as s]
    [hub.templates :as templates]
    [hub.util :as hu]
    [hub.views :as v]
    [ring.middleware.anti-forgery :as anti-forgery]))

(defn authenticate [{:keys [params/email
                            params/accept
                            hub/base-url
                            mailgun/mock
                            jwt/secret]
                     :as sys}]
  (let [token (fe/jwt-encrypt
                {:email (some-> email str/trim)
                 :exp-in (* 60 60 24 3)}
                secret)
        url (fe/assoc-url (str base-url "/hub/api/authenticate")
              :token token)
        email-data (templates/authenticate
                     {:to email
                      :url url})
        bot (or (empty? email) (not-empty accept))
        email-sent (when-not bot
                     (or mock (fe/send-mailgun sys email-data)))]
    (when email-sent
      (println (str "Click here to sign in as " email ": " url)))
    (fv/render v/authenticate
      {:email email
       :success email-sent}
      {:status (if email-sent 200 400)})))

(defn verify-token [{:keys [flub.crux/db
                            flub.crux/node
                            hub.middleware/secure
                            params/token
                            session
                            jwt/secret] :as sys}]
  (if-some [{:keys [email]} (fe/jwt-decrypt token secret)]
    (let [existing-uid (ffirst
                         (crux/q @db
                           '{:find [user]
                             :in [input-email]
                             :where [[user :user/email email]
                                     [(hub.util/email= email input-email)]]}
                           email))
          new-uid (java.util.UUID/randomUUID)]
      (when-not existing-uid
        (flux/submit-tx sys
          {[:user new-uid] {:user/email email}}))
      {:status 302
       :headers/Location "/hub/"
       :cookies/csrf {:path "/"
                      :max-age (* 60 60 24 90)
                      :same-site :lax
                      :value (force anti-forgery/*anti-forgery-token*)}
       :session-cookie-attrs {:path "/"
                              :http-only true
                              :same-site :lax
                              :secure (boolean secure)
                              :max-age (* 60 60 24 90)}
       :session (assoc session :uid (or existing-uid new-uid))})
    {:status 302
     :headers/Location "/?invalid-token=true"}))

(def routes
  [["/" {:get #(fv/render v/home %)}]
   ["/hub" {}
    ["/api" {}
     ["/authenticate" {:post #(authenticate %)
                       :get #(verify-token %)}]]]])
