(ns hub.components
  (:require
    [flub.core :as flub]
    [flub.components :as c]
    [flub.crux :as fc]
    [flub.middleware :as fm]
    [flub.views :as fv]
    [hub.config :as config]
    [hub.plugins :as plugins]
    [hub.routes :as r]
    [hub.schema :as schema]
    [hub.views :as v]
    [malli.core :as m]
    [malli.registry :as mr]
    [reitit.ring :as reitit]
    [ring.middleware.session.cookie :as cookie]))

(def default-handlers
  [(reitit/create-resource-handler
     {:path "/"})
   (reitit/create-default-handler
     {:not-found          (fn [_] (v/error {:status 404 :msg "Not found."}))
      :method-not-allowed (fn [_] (v/error {:status 405 :msg "Not allowed."}))
      :not-acceptable     (fn [_] (v/error {:status 406 :msg "Not acceptable."}))})])

(defn merge-code [sys]
  (merge sys
    {:flub.reitit/routes r/routes
     :flub.reitit/default-handlers default-handlers
     :flub.malli/registry (mr/composite-registry
                            m/default-registry
                            schema/registry)}))

(defn wrap-plugins [sys]
  (let [plugins (map #(deref (requiring-resolve %)) plugins/plugins)
        routes (mapv (fn [{:keys [routes prefix]}]
                       [(str "/" prefix) {} routes])
                 plugins)]
    (-> sys
      (assoc :hub/plugins plugins)
      (update :flub.reitit/routes into routes))))

(defn on-error [req exc]
  (fv/render v/internal-error req {:status 500}))

(defn wrap-middleware [{:keys [cookie/secret
                               hub.middleware/secure
                               flub.crux/node
                               flub.web/handler]
                        :as sys}]
  (let [store (cookie/cookie-store {:key (flub/base64-decode secret)})]
    (assoc sys :flub.web/handler
      (-> handler
        (fm/wrap-defaults {:session-store store
                           :secure secure
                           :env sys
                           :on-error on-error})
        (fc/wrap-db {:node node})))))

(defn ready [{:keys [flub.jetty/port]
              :or {port 8080}
              :as sys}]
  (println "Jetty running on" (str "http://localhost:" port))
  sys)

(def all-components
  [config/merge-config
   merge-code
   wrap-plugins
   c/nrepl
   fc/start-crux
   c/reitit
   wrap-middleware
   c/jetty
   ready])
