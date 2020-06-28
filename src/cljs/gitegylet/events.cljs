(ns gitegylet.events
  (:require [re-frame.core :as rf]
            [gitegylet.effects :as fx]
            [gitegylet.db :as db]))

(def persist
  "Interceptor to persist database"
  (rf/after db/persist))

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::db/persistence)]
  (fn [{:keys [persisted]}]
    {:db (merge {:repo "."}
                persisted)
     :dispatch [:gitegylet.branches.events/reload]
     ::fx/interval {:action :start
                    :id :status-poll
                    :freq 2000
                    :event [:gitegylet.repo.events/check-status]}}))

(rf/reg-event-fx
  ::send-ipc-message
  (fn [_ [_ message]]
    (let [object {:type :ipc-request
                  :payload message}]
      (js/window.postMessage (clj->js object)))))
