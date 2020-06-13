(ns electron.events
  (:require ["electron" :refer [dialog]]))

(defn ipc-respond
  [event type payload]
  (-> event
      (.-sender)
      (.send "ipc-response" (clj->js {:type type
                                      :payload payload}))))

(defn folder-dialog
  [event _]
  (let [result (->> (clj->js {:title "Choose a repository"
                              :properties ["openDirectory"]})
                    (.showOpenDialog dialog))]
    (.then result
      (fn extract-chosen-folder [result]
        (let [folder (first (.-filePaths result))]
          ; (js/console.log "folder to open" folder)
          (ipc-respond event :open-repo folder))))))

(defn ipc-handler
  [event payload]
  (let [event-handler (case payload
                        "open-repo" folder-dialog
                        identity)]
    (event-handler event payload)))
