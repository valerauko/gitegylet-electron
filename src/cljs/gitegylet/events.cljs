(ns gitegylet.events
  (:require
    [re-frame.core :as rf]
    [cljs.spec.alpha :as s]
    [gitegylet.git :refer [git]]))

(rf/reg-event-fx
  ::initialize-db
  (fn [_ _]
    {:db {:branches []
          :branches-checked []
          :branches-expanded []}}))

(rf/reg-event-db
 ::branch-check
 (fn [db [_ item]]
   (assoc db :branches-checked item)))

(rf/reg-event-db
 ::branch-expand
 (fn [db [_ item]]
   (assoc db :branches-expanded item)))

(rf/reg-event-db
  ::reload-branches
  (fn [db _]
    (assoc db :branches (.localBranches git))))
