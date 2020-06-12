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
                 #(group-branches % new-parents)))))))))

(defn ui
  []
  (let [nodes (->> @(rf/subscribe [::subs/branches])
                   (map #(split % #"/"))
                   (group-branches))
        commits @(rf/subscribe [::subs/commits])
        checked @(rf/subscribe [::subs/branches-checked])
        expanded @(rf/subscribe [::subs/branches-expanded])
        on-check #(rf/dispatch [::events/branch-check %])
        on-expand #(rf/dispatch [::events/branch-expand %])]
    [:div {:id "flex"}
     [:div {:class "branches"}
      [:> CheckboxTree
       {:nodes nodes
        :checked checked
        :expanded expanded
        :only-leaf-checkboxes true
        :on-check on-check
        :on-expand on-expand}]]
     [:div {:class "commits"}
      (->> commits
           (map (fn [msg] [:li msg]))
           (into [:ol]))]]))
