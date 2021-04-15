(ns hub.extra.curate
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.crux :as flux]
    [flub.malli :as flubm]
    [flub.extra :as flex]
    [flub.views :as fv]
    [hub.util :as hu]
    [hub.views :as hviews]
    [lambdaisland.uri :as uri]
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
                :event/type]
   :param-tx/op-key [:enum :crux.tx/delete :crux.tx/put]
   :param-tx/op-val [:not [:map [:crux.tx/fn any?]]]
   :param-tx/operation [:tuple :param-tx/op-key :param-tx/op-val]
   :param-tx [:vector :param-tx/operation]})

(def registry (flubm/registry registry*))

(defn path-for [{:keys [flub.reitit/router]} & args]
  (:path (apply r/match-by-name router args)))

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

(defn normalize-tags [tags]
  (-> (or tags "")
    str/lower-case
    (str/replace #"[^a-z\s]+" "-")
    (str/split #"\s+")
    set))

(defn req->redirect-params [{:keys [params/txredirect params/url]}]
  (when (= ::rate (edn/read-string txredirect))
    {:rating-saved true
     :url url}))

(defn req->tx [{:params/keys [url
                              title
                              description
                              visibility
                              rating
                              tags
                              commentary]
                :keys [hub/user-db]}]
  (let [doc (merge
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
                :item/tags (normalize-tags tags)
                :item/rating (flub/catchall (Long/parseLong rating))
                :item/commentary commentary))]
    {[:item (:crux.db/id doc)] doc}))

(defn doc->form [{:item/keys [uris
                               title
                               description
                               flags
                               rating
                               tags
                               commentary
                               rated-at]
                   :keys [crux.db/id]}]
  {:id id
   :url (first uris)
   :title title
   :description description
   :visibility (if (:private flags) "private" "public")
   :rating (cond
             (:save-for-later flags) "save"
             rating (str rating)
             :default "share")
   :tags (str/join " " (sort tags))
   :commentary commentary
   :rated-at rated-at})

(defc card-row [label contents]
  (list
    [:.text-sm.text-gray-600 label]
    [:.mb-2 contents]))

(defn history-page [{:keys [hub/user-db] :as req}]
  (let [offset 0
        results (crux/q @user-db
                  {:find '[(pull item [*]) rated-at]
                   :where '[[item :item/rated-at rated-at]]
                   :order-by '[[rated-at :desc]]
                   :limit 20
                   :offset 0})]
    (hviews/plugin-base req
      [:.flex.flex-wrap
       (flub/join [:.w-3]
         (for [[item] results
               :let [{:keys [id
                             url
                             title
                             description
                             visibility
                             rating
                             tags
                             commentary
                             rated-at]} (doc->form item)]]
           [:.p-3.bg-white.max-w-84.mb-3
            (card-row "Link"
              (list [:a.link.break-all {:href url :target "_blank"} (or title url)]
                (when-not title
                  [:span.text-gray-600.break-all
                   (str " (" (:host (uri/uri url)) ")")])))
            (when description
              (card-row "Description" description))
            (card-row "Visibility" (str/capitalize visibility))
            (card-row "Rating"
              (case rating
                "save" "Saved for later"
                "share" "Shared without rating"
                (repeat (Long/parseLong rating) "★")))
            (when (not-empty tags)
              (card-row "Tags" tags))
            (when commentary
              (card-row "Commentary" commentary))
            (card-row "Date added"
              (flub/format-date rated-at "dd MMM YYYY"))
            [:hr.my-3]
            [:.flex.justify-end
             (form {:action (path-for req ::tx)
                    :hidden {:tx (pr-str [[:crux.tx/delete id]])
                             :txredirect (pr-str ::history)}
                    :class "contents"}
               [:button.text-red-600.hover:underline "Delete"])
             [:.w-3]
             [:a.btn {:href (flex/assoc-url (path-for req ::rate)
                              "url" url)} "Edit"]]]))])))

(defn rate-page [{:keys [params hub/user-db params/rating-saved]
                  :as req}]
  (let [item (flux/q-entity @user-db [[:item/uris (:url params)]])
        {:keys [url
                title
                description
                visibility
                rating
                tags
                commentary]} (merge
                               (some-> item doc->form)
                               (hu/dissoc-empty params))
        bookmarklet (slurp (io/resource "hub/rate-item-bookmarklet.js"))
        notice (cond
                 rating-saved ["Your rating has been saved."]
                 (not url) ["Tip: drag "
                            [:a.link {:href bookmarklet} "this bookmarklet"]
                            " to your bookmarks menu. Use it to rate the current page."]
                 :default nil)]
    (hviews/plugin-base req
      [:.max-w-prose.bg-white.p-3
       (form {:action (path-for req ::tx)
              :hidden {:txflags "rating-saved"
                       :txredirect (pr-str ::rate)}}
         ; todo add select box for content type (e.g. feed)
         (when notice
           (list
             (into [:div] notice)
             [:hr.my-3]))
         (text-field {:label "URL (required)"
                      :name "url"
                      :type "url"
                      :required "required"
                      :placeholder "https://example.com"
                      :value url})
         [:.h-3]
         (text-field {:label "Title"
                      :name "title"
                      :value title})
         [:.h-3]
         (text-field {:label "Description"
                      :name "description"
                      :textarea true
                      :value description})
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

(defn flux-opts [{:keys [hub/user-node]}]
  {:flub.crux/node @user-node
   :flub.malli/registry registry})

(defn redirect [{::keys [route-name] :as req}]
  {:status 302
   :headers/Location (path-for req route-name)})

(defn tx [{:keys [params/tx
                  params/txredirect
                  params/txflags
                  flub.reitit/router] :as req}]
  (let [route (r/match-by-name router (edn/read-string txredirect))
        path (:path route)
        redirect-ok (get-in route [:data :hub/redirect])
        param-tx (some-> tx edn/read-string)
        tx (or param-tx (req->tx req))]
    (assert redirect-ok)
    (when param-tx
      (flubm/assert :param-tx param-tx {:registry registry}))
    (flux/submit-await-tx (flux-opts req) tx)
    {:status 302
     :headers/location (str (uri/assoc-query* path (req->redirect-params req)))}))

(def routes
  [["" {:middleware [hu/wrap-auth-required]}
    ["/" {:get #(redirect (assoc % ::route-name ::rate))}]
    ["/rate/" {:get #(fv/render rate-page %)
               :name ::rate
               :hub/redirect true
               :hub/title "Submit"
               :hub/order 0}]
    ["/history/" {:name ::history
                  :get #(fv/render history-page %)
                  :hub/redirect true
                  :hub/title "History"
                  :hub/order 1}]
    ["/api/tx" {:name ::tx
                :post #(tx %)}]]])

(def manifest
  {:title "Curate"
   :prefix "hub.curate"
   :routes routes
   :registry registry*
   :refresh (fn [_] nil)})
