(ns gitegylet.views
  (:require [re-frame.core :as rf]
            [gitegylet.events :as events]
            [gitegylet.branches.views :refer [branches]]
            [gitegylet.commits.views :refer [commits]]))

(defn ui
  []
  [:div {:id "main"}
   [:nav {:id "topbar"}
    [:button
     {:on-click #(rf/dispatch [::events/send-ipc-message :open-repo])
      :title "Open repo"}
     "\uf07c"]]
   [:div {:id "flex"}
    (branches)
    (commits)]])
