(ns hub.extra.curate)

(def schema
  {})

(def routes
  ["/" ::foo])

(def manifest
  {:prefix "hub.curate"
   :title "Curate"
   :description "Organize URLs into shareable feeds."
   :icon nil
   :registry {}
   :routes routes})
