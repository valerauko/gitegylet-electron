(ns gitegylet.branches.events
  (:require [re-frame.core :as rf]
            [gitegylet.git :refer [git]]
            [gitegylet.events :refer [persist]]
            [gitegylet.commits.events :as commits]
            [gitegylet.branches.db :refer [branch->map]]))

(rf/reg-cofx
  ::local-branches
  (fn [{{:keys [repo]} :db :as cofx} _]
    (assoc cofx ::local-branches
      (->> repo
           (.localBranches git)
           (map branch->map)))))

(rf/reg-event-fx
  ::reload
  [(rf/inject-cofx ::local-branches)
   persist]
  (fn [{::keys [local-branches] :keys [db] :as cofx}]
    {:db (assoc db :local-branches local-branches)
     :dispatch-n [::commits/reload]}))

(rf/reg-event-db
  ::toggle-selection
  [persist]
  (fn [db [_ name selected selected?]]
    (let [f (if selected? conj disj)]
      (assoc db :branches-selected (f selected name)))))

(rf/reg-event-db
  ::toggle-expansion
  [persist]
  (fn [db [_ name expanded expanded?]]
    (let [f (if expanded? conj disj)]
      (assoc db :folders-expanded (f expanded name)))))
