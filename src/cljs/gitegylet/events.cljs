(ns gitegylet.events
  (:require [re-frame.core :as rf]
            [gitegylet.db :as db]))

(def persist
  "Interceptor to persist database"
  (rf/after db/persist))

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::db/persistence)]
  (fn [{:keys [persisted]}]
    {:db (merge {:repo "."}
                persisted)}))

(rf/reg-event-fx
  ::send-ipc-message
  (fn [_ [_ message]]
    (let [object {:type :ipc-request
                  :payload message}]
      (js/window.postMessage (clj->js object)))))

(rf/reg-event-fx
  ::open-repo
  [persist]
  (fn [cofx [_ folder]]
    ; if the dialog was cancelled folder is going to be empty
    (if folder
      ; override whole db when a new repo is opened
      {:db {:repo folder}
       ; and reload branches
       :dispatch [:gitegylet.branches.events/reload]}
      cofx)))
