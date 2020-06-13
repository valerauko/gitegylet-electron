(ns gitegylet.subs
  (:require
   [re-frame.core :as rf]
   [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::branches-checked
  (fn [db _]
    (:branches-selected db)))

(rf/reg-sub
  ::branches-expanded
  (fn [db _]
    (:folders-expanded db)))

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
  :<- [::branches-selected]
  (fn [[repo-path branches] _]
    (when-not (empty? branches)
      (.commits git repo-path branches))))
