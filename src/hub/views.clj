(ns hub.views
  (:require
    [flub.views :as fv]
    [rum.core :as rum :refer [defc]]))

(defn mstr [s]
  s)

(def default-opts
  #:base{:title "Hubert - All your content in one place"
         :lang "en-US"
         :icon "https://findka.com/favicon-16x16.png"
         :description "All your content in one place."})

(def head*
  [[:link {:rel "stylesheet" :href "/css/main.css"}]
   [:link {:rel "stylesheet" :href "/css/custom.css"}]])

(defc base [{:keys [base/head] :as opts} & contents]
  (let [head (concat head* head)
        opts (merge
               default-opts
               opts
               {:base/head head})]
    (apply fv/base opts contents)))

(def navbar
  [:.bg-rgba-343a40ff.text-white.px-4.py-3.text-3xl
   "Findka Hub"])

(defc home [{:keys [params/error]}]
  (base {}
    navbar
    [:.max-w-prose.p-4
     (when error
       [:.bg-rgba-fecacaff.rounded.p-2.mb-4
        (case error
          "invalid-token" "Invalid link. Try signing in again."
          "unauthenticated" "You must be signed in to access that page."
          "There was an error.")])
     [:.text-lg.mb-2
      "Findka Hub is a collection of open-source plugins that help you
      consume and share content."]
     [:form
      [:fieldset.flex
       [:input.input-text.flex-grow-1.mr-3
        {:placeholder "Email",
         :type "email"
         :name "email"}]
       [:input {:type "checkbox" :name "accept" :hidden "hidden"}]
       [:button.btn {:type "submit"}
        "Authenticate"]]]
     [:div "See "
      [:a.link {:href "https://github.com/jacobobryant/hubert" :target "_blank"}
       "github.com/jacobobryant/hubert"] " for more info."]]
    [:script {:src "/js/home.js"}]))

(defc authenticate [{:keys [email success]}]
  (base {}
    navbar
    [:.max-w-prose.p-4
     (if success
       (list
         [:.text-lg.font-bold "One more step"]
         [:p "We've sent a signin link to " [:strong email]
          ". Please check your inbox."])
       (list
         [:.text-lg.font-bold "Something went wrong"]
         [:p "We weren't able to send a signin link to " [:strong email]
          ". If you're sure this is a valid address, please "
          [:a.link {:href "/"} "try again"] "."]))]))

(defc internal-error [_]
  (base {}
    navbar
    [:.max-w-prose.p-4
     [:.text-lg.font-bold "Internal error"]
     [:p "Something went wrong."]]))

(defn error [{:keys [status msg]}]
  {:status status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (rum/render-static-markup
           (base {}
             navbar
             [:.p-4
              msg]))})

(defc hub-home [{:keys [session/uid]}]
  (base {}
    navbar
    [:.p-4 [:p "yo"]]))

(comment
  (time
    (let []
    (require 'hub.css :reload)
    @(requiring-resolve 'hub.css/foo)
    ))
  )
