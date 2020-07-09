(ns gitegylet.modal.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::modal
  (fn [{:keys [modal] :as db} _]
    modal))
