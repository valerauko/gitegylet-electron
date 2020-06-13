(ns gitegylet.events
  (:require
    [re-frame.core :as rf]
    [cljs.spec.alpha :as s]
    [gitegylet.git :refer [git]]))

(rf/reg-event-fx
  ::initialize-db
  (fn [_ _]
    {:db {:repo "."}}))

(rf/reg-event-db
 ::branch-check
 (fn [db [_ item]]
   (assoc db :branches-checked item)))

(rf/reg-event-db
 ::branch-expand
 (fn [db [_ item]]
   (assoc db :branches-expanded item)))

(rf/reg-event-db
 ::send-ipc-message
 (fn [db [_ message]]
   (let [object {:type :ipc-request
                 :payload message}]
     (js/window.postMessage (clj->js object)))
   db))

(rf/reg-event-db
 ::open-repo
 (fn [db [_ folder]]
   (assoc db :repo folder)))
