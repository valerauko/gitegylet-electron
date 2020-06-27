(ns gitegylet.branches.events
  (:require [re-frame.core :as rf]
            [gitegylet.events :refer [persist]]))

(rf/reg-event-db
  ::branch-toggle
  [persist]
  (fn [db [_ name selected?]]
    (let [f (if selected? conj disj)
          old (get db :branches-selected #{})]
      (assoc db :branches-selected (f old)))))
