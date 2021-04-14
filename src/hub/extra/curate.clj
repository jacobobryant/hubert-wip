(ns hub.extra.curate
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [flub.core :as flub]
    [flub.crux :as flux]
    [flub.malli :as flubm]
    [flub.views :as fv]
    [hub.util :as hu]
    [hub.views :as hviews]
    [reitit.core :as r]
    [ring.middleware.anti-forgery :as anti-forgery]
    [rum.core :as rum :refer [defc]]))

(def registry*
  {:item/uris         [:and [:vector :string] [:not empty?]]
   :item/rated-at     inst?
   :item/title        :string
   :item/description  :string
   :item/image        :string
   :item.author/name  :string
   :item.author/url   :string
   :item/author       [:map
                       :item.author/name
                       :item.author/url]
   :item/authors      [:vector :item/author]
   :item/feed-url     :string
   :item/content-url  :string
   :item/published-at inst?
   :item/flags        [:set :keyword] ; :subscribe :save-for-later :private
   :item/formats      [:set :keyword]
   :item/tags         [:set :string]
   :item/rating       [:and number? [:>= 1] [:<= 5]]
   :item/commentary   :string
   :item/id           :uuid
   :item [:map
          [:crux.db/id :item/id]
          :item/uris
          :item/rated-at
          [:item/title        {:optional true}]
          [:item/description  {:optional true}]
          [:item/image        {:optional true}]
          [:item/authors      {:optional true}]
          [:item/feed-url     {:optional true}]
          [:item/content-url  {:optional true}]
          [:item/published-at {:optional true}]
          [:item/flags        {:optional true}]
          [:item/formats      {:optional true}]
          [:item/tags         {:optional true}]
          [:item/rating       {:optional true}]
          [:item/commentary   {:optional true}]]
   :event/item :item/id
   :event/occurred-at inst?
   :event/type keyword?
   :event/id   :uuid
   :event      [:map
                [:crux.db/id :event/id]
                :event/occurred-at
                :event/item
                :event/type]})

(def registry (flubm/registry registry*))

(defn path-for [{:keys [flub.reitit/router]} route-name]
  (:path (r/match-by-name router route-name)))

(defn hidden-fields [kvs]
  (for [[k v] kvs]
    [:input {:type "hidden" :name k :value v}]))

(defc form [opts & body]
  [:form (merge
           {:method "post"}
           (select-keys opts [:class :style :action :enctype]))
   (hidden-fields (assoc (:hidden opts)
                    "__anti-forgery-token" anti-forgery/*anti-forgery-token*))
   body])

(defn text-field [{:keys [name label textarea] :as opts}]
  (list
    [:div [:label {:for name} label]]
    [:.h-1]
    [(if textarea
       :textarea
       :input)
     (-> opts
       (dissoc :label :textarea)
       (update :class concat ["input-text"])
       (assoc :id name))]))

(defc select
  [{:keys [options default name label]}]
  (list
    [:div [:label {:for name} label]]
    [:.h-1]
    [:div
     [:span.relative
      [:select
       {:id name
        :name name
        :class ["appearance-none" "border" "border-gray-400" "py-1" "pl-2" "pr-6"
                "rounded" "focus:outline-none" "focus:shadow-outline"
                "bg-white" "text-black"]}
       (for [[value label] options]
         [:option (flub/assoc-some
                    {:value value}
                    :selected (when (= value default) "selected"))
          label])]
      [:.pointer-events-none.absolute.inset-y-0.right-0.flex.items-center.px-2.text-gray-700
       [:svg.fill-current.h-4.w-4
        {:viewbox "0 0 20 20"}
        [:path
         {:d
          "M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"}]]]]]))

(defn history-page [req]
  (hviews/plugin-base req
    [:div "History page"]))

(defn rate-page [{:params/keys [url
                                title
                                description
                                visibility
                                rating
                                tags
                                commentary]
                  ::keys [rating-saved]
                  :as req}]
  ; todo prefill if doc exists
  (let [feed-url nil
        bookmarklet (slurp (io/resource "hub/rate-item-bookmarklet.js"))
        notice (cond
                 rating-saved ["Your rating has been saved."]
                 (not url) ["Tip: drag "
                            [:a.link {:href bookmarklet} "this bookmarklet"]
                            " to your bookmarks menu. Use it to rate the current page."]
                 :default nil)]
    (hviews/plugin-base req
      [:.max-w-prose
       (form {:action (path-for req ::rate)}
         ; todo add select box for content type (e.g. feed)
         (when notice
           (into [:.bg-gray-200.p-3.rounded.mb-3] notice))
         ; todo update stuff after url change
         (text-field {:label "URL (required)"
                      :name "url"
                      :type "url"
                      :required "required"
                      :placeholder "https://example.com"
                      :value url})
         [:.h-3]
         (text-field {:label "Title"
                      :name "title"
                      :value (flub/ellipsize 80 title)})
         [:.h-3]
         (text-field {:label "Description"
                      :name "description"
                      :textarea true
                      :value (flub/ellipsize 400 description)})
         [:.h-3]
         (select {:label "Visibility"
                  :options [["public" "Public"]
                            ["private" "Private"]]
                  :default (or visibility "public")
                  :name "visibility"})
         [:.h-3]
         (select {:label "Rating"
                  :name "rating"
                  :default (or rating "share")
                  :options [["share" "Share without rating"]
                            ["save" "Save for later"]
                            ["1" "★☆☆☆☆"]
                            ["2" "★★☆☆☆"]
                            ["3" "★★★☆☆"]
                            ["4" "★★★★☆"]
                            ["5" "★★★★★"]]})
         [:.h-1]
         [:.text-sm.text-gray-600
          "If you select \"Save for later\", this item will be added to "
          "your queue, and it won't be publicly visible to others until you change the rating."]
         [:.h-3]
         (text-field {:label "Tags (space separated)"
                      :name "tags"
                      :placeholder "philosophy cheese linux"
                      :value tags})
         [:.h-3]
         (text-field {:label "Commentary"
                      :name "commentary"
                      :textarea true
                      :value commentary})
         [:.h-5]
         [:div [:button.btn.w-full {:type "submit"} "Save"]])])))

(defn normalize-tags [tags]
  (-> (or tags "")
    str/lower-case
    (str/replace #"[^a-z\s]+" "-")
    (str/split #"\s+")
    set))

(defn submit-rating [{:params/keys [url
                                    title
                                    description
                                    visibility
                                    rating
                                    tags
                                    commentary]
                      :keys [hub/user-node hub/user-db]
                      :as req}]
  (let [tags (normalize-tags tags)
        doc (merge
              {:crux.db/id (java.util.UUID/randomUUID)
               :item/rated-at (java.util.Date.)}
              (select-keys
                (flux/q-entity @user-db [[:item/uris url]])
                [:crux.db/id :item/rated-at])
              (hu/assoc-not-empty
                {:item/uris [url]}
                :item/title title
                :item/description description
                :item/flags (cond-> #{}
                              (= visibility "private") (conj :private)
                              (= rating "save") (conj :save-for-later))
                :item/formats #{:article}
                :item/tags tags
                :item/rating (flub/catchall (Long/parseLong rating))
                :item/commentary commentary))]
    (flux/submit-tx
      {:flub.crux/node @user-node
       :flub.malli/registry registry}
      {[:item (:crux.db/id doc)] doc})
    (fv/render rate-page (assoc req
                           ::rating-saved true
                           :params/tags (str/join " " tags)))))

(defn redirect [{::keys [route-name] :as req}]
  {:status 302
   :headers/Location (path-for req route-name)})

(def routes
  [["" {:middleware [hu/wrap-auth-required]}
    ["/" {:get #(redirect (assoc % ::route-name ::rate))}]
    ["/rate/" {:get #(fv/render rate-page %)
               :post #(submit-rating %)
               :name ::rate
               :hub/title "Submit"
               :hub/order 0}]
    ["/history/" {:name ::history
                  :get #(fv/render history-page %)
                  :hub/title "History"
                  :hub/order 1}]]])

(def manifest
  {:title "Curate"
   :prefix "hub.curate"
   :routes routes
   :registry registry*
   :refresh (fn [_] nil)})
