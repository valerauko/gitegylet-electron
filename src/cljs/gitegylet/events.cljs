(ns gitegylet.events
  (:require
    [re-frame.core :as rf]
    [cljs.spec.alpha :as s]
    [gitegylet.git :refer [git]]
    [gitegylet.db :as db]))

(def ->local-storage
  "Interceptor to persist database"
  (rf/after db/->local-storage))

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::db/local-storage)]
  (fn [{:keys [db local-storage]}]
    {:db (merge {:repo "."} local-storage)}))

(rf/reg-event-db
 ::branch-select
 [->local-storage]
 (fn [db [_ item]]
   (assoc db :branches-selected item)))

(rf/reg-event-db
 ::folder-expand
 [->local-storage]
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
 [->local-storage]
 (fn [db [_ folder]]
   ; if the dialog was cancelled folder is going to be empty
   (if folder
     ; override whole db when a new repo is opened
     {:repo folder}
     db)))
