(ns gitegylet.branches.subs
  (:require [re-frame.core :as rf]
            [gitegylet.subs]
            [clojure.string :refer [join split]]
            [gitegylet.branches.db :refer [branch->map]]
            [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::locals
  (fn [{:keys [local-branches]} _]
    local-branches))

(rf/reg-sub
  ::names
  :<- [::locals]
  (fn [branches]
    (map :full-name branches)))

;; selection

(rf/reg-sub
  ::-selected
  (fn [db _]
    (:branches-selected db)))

(rf/reg-sub
  ::selected
  :<- [::locals]
  :<- [::-selected]
  (fn [[branches selected] _]
    (let [selected-filter (if (empty? selected)
                            (constantly true)
                            (into #{} selected))]
      (filter #(some-> % :full-name (selected-filter)) branches))))

(rf/reg-sub
  ; use the branches-selected key in the db if present else select every branch
  ::names-selected
  :<- [::names]
  :<- [::-selected]
  (fn [[all-names selected] _]
    (or selected (into #{} all-names))))

;; tree expansion

(rf/reg-sub
  ::-folders-expanded
  (fn [db _]
    (:folders-expanded db)))

(rf/reg-sub
  ; use the folders-expanded key in the db if present else expand every folder
  ::folders-expanded
  :<- [::names]
  :<- [::-folders-expanded]
  (fn [[all-names expanded] _]
    (if expanded
      expanded
      (into #{}
        (comp (map #(-> % (split #"/") (butlast)))
              (filter (complement empty?))
              (map #(join "/" %)))
        all-names))))
