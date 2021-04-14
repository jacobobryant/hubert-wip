(ns hub.views
  (:require
    [clojure.string :as str]
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.views :as fv]
    [reitit.core :as r]
    [rum.core :as rum :refer [defc]]))

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
  [:.bg-rgba-343a40ff.text-white.p-3.text-2xl
   [:a.leading-none {:href "/"}
    "Findka Hub"]])

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

(defc plugin-navbar [{:keys [flub.crux/db
                             hub/plugins
                             uri
                             flub.reitit/router
                             session/uid]
                      :as req}]
  (let [routes (into {} (r/routes router))]
    [:.bg-rgba-343a40ff.text-white.p-3
     [:.sm:flex.items-baseline
      [:.text-2xl
       [:a.leading-none {:href "/?no-redirect=true"}
        "Findka Hub"]]
      [:.flex-grow.h-1]
      [:div
       (:user/email (crux/entity @db uid))
       " | "
       [:a.hover:underline {:href "/hub/api/signout"}
        "sign" fv/nbsp "out"]]]
     [:.h-4]
     [:.flex.flex-wrap
      (flub/join [:.w-3]
        (for [{:keys [title prefix]} plugins
              :let [path (str "/" prefix "/")
                    active (str/starts-with? uri path)]
              :when (contains? routes path)]
          [(if active
             :a.bg-white.text-black.rounded.px-2.py-1
             :a.p-1.hover:underline)
           {:href path} title]))]]))

(defc plugin-breadcrumbs [{:keys [hub/plugins
                                  flub.reitit/router
                                  uri]
                           :as req}]
  (let [prefix (second (str/split uri #"/"))
        routes (->> (r/routes router)
                 (map (fn [[path opts]]
                        (assoc opts :path path)))
                 (filter (fn [{:keys [path hub/title]}]
                           (and
                             title
                             (str/starts-with? path (str "/" prefix "/")))))
                 (sort-by (juxt #(:hub/order % 100) :path)))]
    [:.mb-6
     (flub/join " | "
       (for [{:keys [path hub/title]} routes]
         (if (= uri path)
           [:strong title]
           [:a.link {:href path} title])))]))

(defc plugin-base [req & body]
  (base req
    (plugin-navbar req)
    [:.p-3
     (plugin-breadcrumbs req)
     body]))
