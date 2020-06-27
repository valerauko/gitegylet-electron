(ns gitegylet.views.branches
  (:require [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            [gitegylet.branches.events :as events]
            [gitegylet.subs :as subs]))

(defn branch->map
  [branch]
  {:full-name (.-name branch)
   :commit-id (.-commitId branch)
   :head? (.-isHead branch)
   :ahead-behind (.-aheadBehind branch)})

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
   (map (fn [node]
          (if-let [children (:children node)]
            [:li (:label node) (layer index children)]
            (let [full-name (:value node)
                  branch (index full-name)
                  selected? (selected full-name)]
              [:li
               [:input {:type "checkbox"
                        :checked selected?
                        :on-change
                        #(rf/dispatch [::events/branch-toggle
                                       full-name
                                       (not selected?)])}]
               [:label {:style {:font-weight
                                (if (:head? branch) "bold" "normal")}}
                (:label node)]]))))
   nodes))

(defn branches
  []
  (let [branches (map branch->map @(rf/subscribe [::subs/branches]))
        indexed (index-by :full-name branches)
        nodes (group-branches (map #(split (:full-name %) #"/") branches))
        selected @(rf/subscribe [::subs/branch-names-selected])
        expanded @(rf/subscribe [::subs/folders-expanded])]
    [:div {:class "branches"}
     (layer indexed nodes selected expanded)]))
