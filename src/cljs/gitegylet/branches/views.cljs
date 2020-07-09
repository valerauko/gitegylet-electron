(ns gitegylet.branches.views
  (:require [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            [gitegylet.branches.events :as events]
            [gitegylet.branches.subs :as subs]))

(defn create-branch-modal
  [commit-id]
  [:form
   {:method "dialog"
    :on-submit (fn [e]
                 (let [input (-> e .-target .-children (.item 0) .-value)]
                   (rf/dispatch [::events/create commit-id input])))}
   [:input {:type "text"
            :placeholder "Enter name for new branch"}]])

(defn index-by
  [f coll]
  (reduce
    (fn [aggr item] (assoc aggr (f item) item))
    {}
    coll))

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

(defn layer
  [index nodes selected expanded]
  (into
   [:ol]
   (map
    (fn [node]
      (let [full-name (:value node)]
        (if-let [children (:children node)]
          (let [expanded? (boolean (expanded full-name))]
            [:li
             {:class (conj ["folder"] (if expanded? "expanded" "closed"))}
             [:span
              {:class "label"}
              [:span
               {:class "name"
                :on-click
                #(rf/dispatch [::events/toggle-expansion
                               full-name
                               expanded
                               (not expanded?)])}
               (:label node)]]
             (layer index children selected expanded)])
          (let [branch (index full-name)
                selected? (boolean (selected full-name))
                [ahead behind] (:ahead-behind branch)]
            [:li
             {:class (-> ["leaf"]
                         (conj (if (:head? branch) "head" "local"))
                         (conj (if selected? "visible" "hidden")))
              :on-double-click
              #(rf/dispatch [::events/checkout full-name])}
             [:span
              {:class "label"}
              [:span
               {:class "visibility-toggle"
                :on-click
                #(rf/dispatch [::events/toggle-selection
                               full-name
                               selected
                               (not selected?)])}
               (if selected? "\uf06e" "\uf070")]
              [:span
               {:class "name"}
               (:label node)]
              (when (or (pos? ahead) (pos? behind))
                [:span
                 {:class "ab"}
                 (when (pos? ahead) [:span {:class "ahead"} ahead])
                 (when (pos? behind) [:span {:class "behind"} behind])])]])))))
   nodes))

(defn branches
  []
  (let [branches @(rf/subscribe [::subs/locals])
        indexed (index-by :full-name branches)
        nodes (group-branches (map #(split (:full-name %) #"/") branches))
        selected @(rf/subscribe [::subs/names-selected])
        expanded @(rf/subscribe [::subs/folders-expanded])]
    [:div {:class "branches"}
     (layer indexed nodes selected expanded)]))
