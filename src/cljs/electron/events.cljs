(ns electron.events)

(defn ipc-respond
  [event payload]
  (-> event
      (.-sender)
      (.send "ipc-response" (clj->js payload))))

(defn ipc-handler
  [event payload]
  (let [event-handler (case payload
                        identity)]
    (event-handler event payload)))
