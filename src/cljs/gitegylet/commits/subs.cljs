(ns gitegylet.commits.subs
  (:require [re-frame.core :as rf]
            [gitegylet.branches.subs :as branches]
            [gitegylet.commits.db :refer [commit->map]]))

(rf/reg-sub
  ::commits
  (fn [{:keys [visible-commits] :as db} _]
    visible-commits))

(rf/reg-sub
  ::head
  (fn [{:keys [head]} _]
    head))

(rf/reg-sub
  ::selected
  (fn [{:keys [selected-commit]}]
    selected-commit))
