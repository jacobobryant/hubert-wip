(ns hub.templates
  (:require
    [rum.core :as rum]))

(defn authenticate [{:keys [to url]}]
  {:to to
   :subject "Sign in to Findka Hub"
   :html (rum/render-static-markup
           [:html
            [:body
             [:div
              [:p [:a {:href url :target "_blank"} "Click here to sign in"] " to Findka Hub."]
              [:p "If you did not request this link, you can ignore this email."]]]])
   :text (str
           "Click here to sign in to Findka Hub: " url "\n\n"
           "If you did not request this link, you can ignore this email.")})
