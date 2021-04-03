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
  [[:link {:rel "stylesheet"
           :href "https://unpkg.com/purecss@2.0.5/build/pure-min.css"
           :crossorigin "anonymous"}]
   [:link {:rel "stylesheet"
           :href "https://unpkg.com/purecss@2.0.5/build/grids-responsive-min.css"
           :crossorigin "anonymous"}]])

(defc base [{:keys [base/head] :as opts} & contents]
  (let [head (concat head* head)
        opts (merge
               default-opts
               opts
               {:base/head head})]
    (apply fv/base opts contents)))

(def navbar
  [:div {:style {:background-color "#343a40"
                 :padding "0.75rem 1rem"
                 :color "white"
                 :font-size "2rem"}}
   "Findka Hub"])

(defc home [req]
  (base {}
    navbar
    [:div {:style {:max-width "65ch"
                   :padding "1rem"}}
     [:div {:style {:font-size "1.1rem"
                    :margin-bottom "0.5rem"}}
      "Findka Hub is a collection of open-source plugins that help you
      consume and share content."]
     [:form.pure-form
      [:fieldset {:style {:display "flex"}}
       [:input {:style {:flex-grow "1"
                        :margin-right "0.75rem"}
                :placeholder "Email", :type "email"
                :name "email"}]
       [:input {:type "checkbox" :name "accept" :hidden "hidden"}]
       [:button.pure-button.pure-button-primary
        {:type "submit"}
        "Authenticate"]]]
     [:div "See "
      [:a {:href "https://github.com/jacobobryant/hubert" :target "_blank"}
       "github.com/jacobobryant/hubert"] " for more info."]]
    [:script {:src "/js/home.js"}]))

(defc authenticate [{:keys [email success]}]
  (base {}
    navbar
    [:div {:style {:max-width "65ch"
                   :padding "1rem"}}
     (if success
       (list
         [:div [:strong "One more step"]]
         [:p "We've sent a signin link to " [:strong email]
          ". Please check your inbox."])
       (list
         [:div [:strong "Something went wrong"]]
         [:p "We weren't able to send a signin link to " [:strong email]
          ". If you're sure this is a valid address, please "
          [:a {:href "/"} "try again"] "."]))]))

(defc internal-error [_]
  (base {}
    navbar
    [:div {:style {:max-width "65ch"
                   :padding "1rem"}}
     [:div [:strong "Internal error"]]
     [:p "Something went wrong."]]))

(defn error [{:keys [status msg]}]
  {:status status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (rum/render-static-markup
           (base {}
             navbar
             [:div {:style {:padding "1rem"}}
              msg]))})
