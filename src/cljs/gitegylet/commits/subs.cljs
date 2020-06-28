(ns gitegylet.commits.subs
  (:require [re-frame.core :as rf]
            [gitegylet.subs]
            [gitegylet.branches.subs :as branches]
            [gitegylet.git :refer [git]]
            [gitegylet.commits.db :refer [commit->map]]))

(rf/reg-sub
  ::commits
  (fn [{:keys [visible-commits] :as db} _]
    visible-commits))

(rf/reg-sub
  ::head
  :<- [:gitegylet.subs/repo]
  (fn [repo-path _]
    (commit->map (.head git repo-path))))
