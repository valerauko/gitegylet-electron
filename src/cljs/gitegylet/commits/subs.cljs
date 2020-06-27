(ns gitegylet.commits.subs
  (:require [re-frame.core :as rf]
            [gitegylet.subs]
            [gitegylet.branches.subs :as branches]
            [gitegylet.git :refer [git]]
            [gitegylet.commits.db :refer [commit->map]]))

(rf/reg-sub
  ::commits
  :<- [:gitegylet.subs/repo]
  :<- [::branches/names-selected]
  (fn [[repo-path branch-names] _]
    (when-not (empty? branch-names)
      (->> branch-names
           (clj->js)
           (.commits git repo-path)
           (map commit->map)))))

(rf/reg-sub
  ::head
  :<- [:gitegylet.subs/repo]
  (fn [repo-path _]
    (commit->map (.head git repo-path))))
