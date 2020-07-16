(ns gitegylet.commits.subs
  (:require [re-frame.core :as rf]
            [gitegylet.branches.subs :as branches]
            [gitegylet.commits.db :refer [commit->map]]))

(rf/reg-sub
  ::commits
  (fn [{:keys [visible-commits] :as db}]
    visible-commits))

(rf/reg-sub
  ::head
  (fn [{:keys [head]}]
    head))

(rf/reg-sub
  ::selected
  (fn [{:keys [selected-commit]}]
    selected-commit))

(rf/reg-sub
  ::diff-files
  (fn [db [_ commit-id]]
    (get-in db [:diff-files commit-id])))
