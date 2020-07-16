(ns gitegylet.commits.events
  (:require [re-frame.core :as rf]
            [gitegylet.repo.db :refer [status->map]]
            [gitegylet.git :refer [git]]
            [gitegylet.inject :as inject]
            [gitegylet.events :refer [persist]]
            [gitegylet.branches.subs :as branches]
            [gitegylet.commits.db :refer [commit->map]]))

(rf/reg-cofx
  ::head
  (fn [{{:keys [repo]} :db :as cofx}]
    (assoc cofx ::head (commit->map (.head git repo)))))

(rf/reg-event-fx
  ::reload-head
  [(rf/inject-cofx ::head)
   persist]
  (fn [{::keys [head] :keys [db]}]
    {:db (assoc db :head head)}))

(rf/reg-cofx
  ::visible-commits
  (fn [{names-selected ::branches/names-selected
        {:keys [repo]} :db
        :as cofx} _]
    (if-not (empty? names-selected)
      (->> names-selected
           (clj->js)
           (.commits git repo)
           (map commit->map)
           (assoc cofx ::visible-commits))
      cofx)))

(rf/reg-event-fx
  ::reload
  [(rf/inject-cofx ::inject/sub [::branches/names-selected])
   (rf/inject-cofx ::visible-commits)
   persist]
  (fn [{::keys [visible-commits] :keys [db]}]
    {:db (assoc db :visible-commits (or visible-commits []))
     :dispatch [::reload-head]}))

(rf/reg-event-fx
  ::toggle-select
  [persist]
  (fn [{{:keys [selected-commit] :as db} :db} [_ new-selected]]
    (when-not (= selected-commit new-selected)
      {:db (assoc db :selected-commit new-selected)
       :dispatch [::load-diff-files new-selected]})))

(rf/reg-event-fx
  ::load-diff-files
  [persist]
  (fn [{{:keys [repo] :as db} :db} [_ commit-id]]
    (when-let [files (and commit-id (.commitDiff git repo commit-id))]
      {:db (assoc-in db [:diff-files commit-id]
                     (map status->map files))})))
