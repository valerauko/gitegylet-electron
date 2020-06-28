(ns gitegylet.repo.events
  (:require [re-frame.core :as rf]
            [gitegylet.git :refer [git]]
            [gitegylet.events :refer [persist]]
            [gitegylet.repo.db :refer [status->map]]))

(rf/reg-event-fx
  ::check-status
  [persist]
  (fn [{{:keys [repo] :as db} :db} _]
    {:db (assoc db :statuses
                (->> (.statuses git repo)
                     (map status->map)))}))

(rf/reg-event-fx
  ::open-repo
  [persist]
  (fn [cofx [_ folder]]
    ; if the dialog was cancelled folder is going to be empty
    (if folder
      ; override whole db when a new repo is opened
      {:db {:repo folder}
       ; and reload branches
       :dispatch [:gitegylet.branches.events/reload]}
      cofx)))
