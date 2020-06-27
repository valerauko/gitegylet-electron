(ns gitegylet.branches.subs
  (:require [re-frame.core :as rf]
            [clojure.string :refer [join split]]
            [gitegylet.branches.db :refer [branch->map]]
            [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::-branches-selected
  (fn [db _]
    (get db :branches-selected #{})))

(rf/reg-sub
  ::-folders-expanded
  (fn [db _]
    (get db :folders-expanded #{})))

(rf/reg-sub
  ::repo
  (fn [db _]
    (:repo db)))

(rf/reg-sub
  ::branches
  :<- [::repo]
  (fn [repo-path _]
    (map branch->map (.localBranches git repo-path))))

(rf/reg-sub
  ::branch-names
  :<- [::branches]
  (fn [branches _]
    (map :full-name branches)))

(rf/reg-sub
  ::branches-selected
  :<- [::branches]
  :<- [::-branches-selected]
  (fn [[branches selected] _]
    (let [selected-filter (if (empty? selected)
                            (constantly true)
                            (into #{} selected))]
      (filter #(some-> % (.-name) (selected-filter)) branches))))

(rf/reg-sub
  ; use the branches-selected key in the db if present else select every branch
  ::branch-names-selected
  :<- [::branch-names]
  :<- [::-branches-selected]
  (fn [[branches selected] _]
    (or selected (into #{} branches))))

(rf/reg-sub
  ; use the folders-expanded key in the db if present else expand every folder
  ::folders-expanded
  :<- [::branch-names]
  :<- [::-folders-expanded]
  (fn [[branches expanded] _]
    (if expanded
      expanded
      (into #{}
        (comp (map #(-> % (split #"/") (butlast)))
              (filter (complement empty?))
              (map #(join "/" %)))
        branches))))
