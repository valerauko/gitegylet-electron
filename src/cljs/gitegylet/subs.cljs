(ns gitegylet.subs
  (:require
   [re-frame.core :as rf]
   [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::branches-checked
  (fn [db _]
    (:branches-checked db)))

(rf/reg-sub
  ::branches-expanded
  (fn [db _]
    (:branches-expanded db)))

(rf/reg-sub
  ::branches
  (fn [db _]
    (:branches db)))

(rf/reg-sub
  ::commits
  :<- [::branches-checked]
  (fn [branches _]
    (when-not (empty? branches)
      (.commits git branches))))
