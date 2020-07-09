(ns gitegylet.views
  (:require [re-frame.core :as rf]
            [gitegylet.events :as events]
            [gitegylet.branches.views :refer [branches]]
            [gitegylet.commits.views :refer [commits]]
            [gitegylet.modal.views :refer [modal]]))

(defn ui
  []
  [:div {:id "main"}
   [:nav {:id "topbar"}
    [:button
     {:on-click #(rf/dispatch [::events/send-ipc-message :open-repo])
      :title "Open repo"}
     "\uf07c"]
    (let [branches @(rf/subscribe [:gitegylet.branches.subs/names])]
      [:button
       {:on-click #(rf/dispatch [:gitegylet.branches.events/fetch branches])
        :title "Fetch"}
       "\uf021"])]
   [:div {:id "flex"}
    (branches)
    (commits)]
   (modal)])
