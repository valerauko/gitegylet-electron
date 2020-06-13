(ns gitegylet.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            ["react-checkbox-tree" :as CheckboxTree]
            [gitegylet.events :as events]
            [gitegylet.subs :as subs]))

(defn nested-branch-reducer
  [aggr item]
  (let [children (rest item)]
    (if (empty? children)
      aggr
      (update aggr :children conj children))))

(defn group-branches
  ([items] (group-branches items []))
  ([items parents]
   (->> items
        (partition-by first)
        (map
         (fn [folder]
           (let [label (-> folder first first)
                 new-parents (conj parents label)
                 layer (reduce
                         nested-branch-reducer
                         {:label label
                          :value (join "/" new-parents)}
                         folder)]
             (if (empty? (:children layer))
               layer
               (update
                 layer
                 :children
                 #(group-branches % new-parents))))))
        (sort-by :value))))

(defn branches
  []
  (let [nodes (->> @(rf/subscribe [::subs/branch-names])
                   (map #(split % #"/"))
                   (group-branches))
        checked @(rf/subscribe [::subs/branch-names-selected])
        expanded @(rf/subscribe [::subs/folders-expanded])
        on-check #(rf/dispatch [::events/branch-select %])
        on-expand #(rf/dispatch [::events/folder-expand %])]
    [:div {:class "branches"}
     [:> CheckboxTree
      {:nodes nodes
       :checked checked
       :expanded expanded
       :only-leaf-checkboxes true
       :on-check on-check
       :on-expand on-expand}]]))

(defn commits
  []
  (let [commits @(rf/subscribe [::subs/commits])
        branches (->> @(rf/subscribe [::subs/branches-selected])
                      (group-by #(.-commitId %)))]
    (js/console.log (clj->js branches))
    [:div {:class "commits"}
     (->> commits
          (map (fn commit-to-element
                 [commit]
                 (let [relevant-branches (get branches (.-id commit))]
                   [:li
                    (some->> relevant-branches
                      (map (fn [branch]
                             [:span
                              {:class ["branch-label"
                                       (when (.-isHead branch)
                                         "head")]}
                              (-> (.-name branch)
                                  (split #"/")
                                  (last))])))
                    [:span
                     {:class ["message"]}
                     (.-summary commit)]])))
          (into [:ol]))]))

(defn ui
  []
  [:div
   [:button
    {:on-click #(rf/dispatch [::events/send-ipc-message :open-repo])}
    "Open repo"]
   [:div {:id "flex"}
    (branches)
    (commits)]])
