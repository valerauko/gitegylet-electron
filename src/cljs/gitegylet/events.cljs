(ns gitegylet.events
  (:require
    [re-frame.core :as rf]
    [cljs.spec.alpha :as s]
    [gitegylet.git :refer [git]]
    [gitegylet.db :as db]))

(def persist
  "Interceptor to persist database"
  (rf/after db/persist))

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::db/persistence)]
  (fn [{:keys [db persisted]}]
    {:db (merge {:repo "."} persisted)}))

(rf/reg-event-db
 ::branch-select
 [persist]
 (fn [db [_ item]]
   (assoc db :branches-selected item)))

(rf/reg-event-db
 ::folder-expand
 [persist]
 (fn [db [_ item]]
   (assoc db :folders-expanded item)))

(rf/reg-event-db
 ::send-ipc-message
 (fn [db [_ message]]
   (let [object {:type :ipc-request
                 :payload message}]
     (js/window.postMessage (clj->js object)))
   db))

(rf/reg-event-db
 ::open-repo
 [persist]
 (fn [db [_ folder]]
   ; if the dialog was cancelled folder is going to be empty
   (if folder
     ; override whole db when a new repo is opened
     {:repo folder}
     db)))
