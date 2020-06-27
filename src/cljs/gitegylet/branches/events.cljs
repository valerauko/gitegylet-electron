(ns gitegylet.branches.events
  (:require [re-frame.core :as rf]
            [gitegylet.events :refer [persist]]))

(rf/reg-event-db
  ::toggle-selection
  [persist]
  (fn [db [_ name selected selected?]]
    (let [f (if selected? conj disj)]
      (assoc db :branches-selected (f selected name)))))
