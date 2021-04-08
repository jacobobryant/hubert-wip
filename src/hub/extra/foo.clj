(ns hub.extra.foo
  (:require
    [flub.views :as fv]
    [hub.util :as hu]
    [hub.views :as hviews]))

(def schema
  {})

(defn main [req]
  (hviews/plugin-base req
    [:div "Welcome to the Foo plugin"]))

(def routes
  [["" {:middleware [hu/wrap-auth-required]}
    ["/" {:get #(fv/render main %)}]]])

(def manifest
  {:title "Foo"
   :prefix "hub.foo"
   :routes routes
   :registry {}
   :refresh (fn [_] nil)})
