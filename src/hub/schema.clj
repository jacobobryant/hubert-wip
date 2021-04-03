(ns hub.schema)

(def registry
  {:user [:map
          [:crux.db/id :uuid]
          [:user/email :string]]})
