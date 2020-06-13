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
  ::repo
  (fn [db _]
    (:repo db)))

(rf/reg-sub
  ::branches
  :<- [::repo]
  (fn [repo-path _]
    (.localBranches git repo-path)))

(rf/reg-sub
  ::commits
  :<- [::repo]
  :<- [::branches-checked]
  (fn [[repo-path branches] _]
    (when-not (empty? branches)
      (.commits git repo-path branches))))
