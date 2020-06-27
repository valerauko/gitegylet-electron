(ns gitegylet.views
  (:require [re-frame.core :as rf]
            [gitegylet.events :as events]
            [gitegylet.branches.views :refer [branches]]
            [gitegylet.commits.views :refer [commits]]))

(defn ui
  []
  [:div
   [:button
    {:on-click #(rf/dispatch [::events/send-ipc-message :open-repo])}
    "Open repo"]
   [:div {:id "flex"}
    (branches)
    (commits)]])
