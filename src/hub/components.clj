(ns hub.components
  (:require
    [clojure.java.io :as io]
    [crux.api :as crux]
    [flub.core :as flub]
    [flub.components :as c]
    [flub.crux :as flux]
    [flub.malli :as fmal]
    [flub.middleware :as fmid]
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
     :flub.malli/registry (fmal/registry schema/registry)}))

(defn wrap-plugins [sys]
  (let [plugins (map #(deref (requiring-resolve %)) plugins/plugins)
        routes (mapv (fn [{:keys [routes prefix]}]
                       [(str "/" prefix) {} routes])
                 plugins)]
    (-> sys
      (assoc :hub/plugins (sort-by :title plugins))
      (update :flub.reitit/routes into routes))))

(defn on-error [req exc]
  (fv/render v/internal-error req {:status 500}))

(defn start-user-node [{:keys [hub/user-crux-dir]} uid]
  (flux/start-node*
    #::flux{:topology :standalone
            :dir (io/file user-crux-dir (str uid))}))

(defn user-nodes [sys]
  (let [locks (atom #{})
        nodes (atom {})
        lock (Object.)
        get-node (fn [uid]
                   (swap! locks
                     (fn [locks]
                       (if-not (contains? locks uid)
                         (conj locks uid)
                         locks)))
                   (locking (get @locks uid)
                     (when-not (contains? @nodes uid)
                       (swap! nodes assoc uid (start-user-node sys uid)))
                     (get @nodes uid)))
        close-nodes (fn []
                      (doseq [[_ node] @nodes]
                        (.close node)))]
    (-> sys
      (assoc :hub/get-node get-node)
      (update :flub/stop conj close-nodes))))

(defn wrap-user-db [handler]
  (fn [{:keys [hub/get-node session/uid] :as req}]
    (let [node (delay (get-node uid))
          db (delay (crux/db @node))
          req (cond-> req
                uid (merge #:hub{:user-node node
                                 :user-db db}))]
      (handler req))))

(defn wrap-verbose [handler {:keys [verbose]}]
  (if verbose
    (fn [req]
      (let [resp (handler req)]
        (println (:status resp) (:request-method req) (:uri req))
        resp))
    handler))

(defn wrap-middleware [{:keys [cookie/secret
                               hub.middleware/secure
                               hub/verbose
                               flub.crux/node
                               flub.web/handler]
                        :as sys}]
  (let [store (cookie/cookie-store {:key (flub/base64-decode secret)})]
    (assoc sys :flub.web/handler
      (-> handler
        wrap-user-db
        (flux/wrap-db {:node node})
        (fmid/wrap-defaults {:session-store store
                             :secure secure
                             :env sys
                             :on-error on-error})
        (wrap-verbose {:verbose verbose})))))

(defn ready [{:keys [flub.web/port]
              :or {port 8080}
              :as sys}]
  (println "Jetty running on" (str "http://localhost:" port))
  sys)

(def all-components
  [config/merge-config
   merge-code
   wrap-plugins
   c/nrepl
   flux/start-node
   c/reitit
   user-nodes
   wrap-middleware
   c/jetty
   ready])
