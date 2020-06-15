(ns gitegylet.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            ["react-checkbox-tree" :as CheckboxTree]
            [gitegylet.events :as events]
            [gitegylet.subs :as subs]))

(defn inspect
  [thing]
  (js/console.log (str thing)))

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

(defn index-by-id
  [commits]
  (reduce
   (fn id-indexer
     [aggr commit]
     (let [parents (js->clj (.-parents commit))
           commit-id (.-id commit)]
       (assoc aggr commit-id {:id commit-id
                              :author
                              (let [author (.-author commit)]
                                {:name (.-name author)
                                 :email (.-email author)
                                 :md5 (.-md5 author)})
                              :time (.-timestamp commit)
                              :parents parents
                              :children #{}})))
   {}
   commits))

(defn inject-children
  [commits]
  (reduce-kv
   (fn inject-one-commit
     [aggr id commit]
     (reduce
      (fn inject-one-parent
        [to-upd parent-id]
        (if (contains? to-upd parent-id)
          (update-in to-upd [parent-id :children]
                     conj id)
          to-upd))
      aggr
      (:parents commit)))
   commits
   commits))

(defn oldest-commit
  [commits-map ids]
  (reduce
   (fn oldest-finder
     [oldest id]
     (let [candidate (get commits-map id)]
       (if (< (:timestamp candidate) (:timestamp oldest))
         candidate
         oldest)))
   {:timestamp js/Infinity}
   ids))

(defn first-unused
  [used-columns]
  (->> (range)
       (filter (fn [n]
                 (not (contains? used-columns n))))
       (first)))

(defn column-commit-map
  ([commits-map initial-ids]
   (column-commit-map commits-map #{} {} initial-ids true))
  ([commits-map initial-used-columns initial-column-map initial-ids should-recur-parents]
   (loop [used-columns initial-used-columns
          column-map initial-column-map
          ids initial-ids]
     (let [current-id (first ids)
           ; _ (inspect current-id)
           remaining (rest ids)]
       ;; procedural as fuck
       (let [current-commit (get commits-map current-id)
             current-column (get column-map current-id
                                 (first-unused used-columns))
             ;; override!
             ; _ (inspect [0 current-id
             ;             "current-column" current-column
             ;             "(get column-map current-id" (get column-map current-id)
             ;             "(first-unused used-columns)" (first-unused used-columns)])
             column-map (assoc column-map current-id current-column)
             ;; override!
             used-columns (conj used-columns current-column)
             ;; the first parent gets the column if it's not present yet
             first-parent-id (->> current-commit
                                  (:parents)
                                  (first))
             ; _ (inspect [1 current-id "first-parent-id" first-parent-id])
             first-parent (get commits-map first-parent-id)
             ;; override!
             ; _ (inspect [-1 current-id used-columns column-map])
             ;; first-parent-id can be nil if current-commit wasn't in
             ;; column-map. this can happen if it's outside the selected
             ;; commit range
             column-map (if (or (nil? first-parent-id)
                                (not should-recur-parents)
                                (contains? column-map first-parent-id))
                          column-map
                          (assoc column-map
                                 first-parent-id
                                 current-column))
             ; _ (inspect [2 current-id used-columns column-map])
             other-parents (rest (:parents current-commit))
             ;; double override!
             [used-columns column-map]
             (if (and should-recur-parents
                      (not (empty? other-parents)))
               (column-commit-map
                commits-map
                used-columns
                column-map
                ;; intentional. prevents other-parents from overriding the
                ;; column of first-parent
                (:parents current-commit)
                ;; only do one layer of parents
                ;; don't recurse further
                false)
               [used-columns column-map])

             ; _ (inspect [3 current-id used-columns column-map])
             ;; override again!
             used-columns
             (let [first-parent-column (column-map first-parent-id)]
               (if (and should-recur-parents
                        (not= current-column first-parent-column))
                 (disj used-columns current-column)
                 used-columns))

             ; _ (inspect [4 current-id used-columns column-map])
             ]
         (if-not (empty? remaining)
           (recur
             used-columns
             column-map
             remaining)
           [used-columns column-map]))))))

(def colors
  ;; TODO: redesign colors. should come up with a palette based on ffc806
  (->> ["#d90171"
        "#cd0101"
        "#f25d2e"
        "#f2ca33"
        "#7bd938"
        "#15a0bf"
        "#0669f7"
        "#8e00c2"
        "#c517b6"]
       (map-indexed (fn [i v] [i v]))
       (into {})))

(defn color
  ([i] (color i -1))
  ([i head]
   (let [hue (rem (+ 47 (* (- i head) 45)) 360)]
     (str "hsl(" hue ", "
                 (if (= i head) 100 60) "%, "
                 (if (= i head) 50 40) "%)"))))

(defn append [item ls] (concat ls (list item)))

(defn commits
  []
  (let [commits @(rf/subscribe [::subs/commits])
        head @(rf/subscribe [::subs/head])
        branches @(rf/subscribe [::subs/branches-selected])
        indexed-branches (group-by #(.-commitId %) branches)]
    [:div {:class "commits"}
     (let [ordered-ids (->> commits (map #(.-id %)) (into []))
           reverse-index (->> ordered-ids
                              (map-indexed (fn [i v] [v i]))
                              (into {}))
           commits-map (->> commits
                            (index-by-id)
                            (inject-children))
           icons (->> ordered-ids
                      (reduce
                       (fn author-md5-mapper [aggr id]
                         (if (contains? aggr id)
                           aggr
                           (let [hash (get-in commits-map [id :author :md5])
                                 icon-size 24]
                             (assoc aggr id
                                    [:pattern
                                     {:key (gensym)
                                      :id hash
                                      :width icon-size
                                      :height icon-size
                                      :patternContentUnits "objectBoundingBox"}
                                     [:image
                                      {:width 1
                                       :height 1
                                       :fill "#1a1d21"
                                       :href
                                       ;; TODO: CSP
                                       (str "https://www.gravatar.com/avatar/"
                                            hash
                                            "?s=" icon-size "&d=retro")}]]))))
                       {})
                      (vals)
                      (into [:defs]))]
       (let [[_ column-map] (column-commit-map commits-map ordered-ids)
             column-count (->> column-map vals (apply max) inc)
             columns (range column-count)
             head-col (get column-map (.-id head))
             canvas-em-height (* 2 (count ordered-ids))
             svg-header [:svg {:style
                               {:width (str (* 2 column-count) "em")
                                :height (str canvas-em-height "em")}}
                         icons]]
         (->> ordered-ids
              (map-indexed
               (fn commit-drawer [idx id]
                 (let [commit (get commits-map id)
                       parent-ids (:parents commit)
                       first-parent-id (first parent-ids)
                       merge? (> (count parent-ids) 1)
                       column (get column-map id)
                       commit-x (+ 16 (* 32 column))
                       commit-y (+ 16 (* 32 idx))
                       commit-color (color column head-col)
                       circle
                       [:circle {:key (gensym)
                                 :r (if merge? 6 12)
                                 :cx commit-x
                                 :cy commit-y
                                 :stroke-width 2.5
                                 :stroke commit-color
                                 :fill (if merge?
                                         commit-color
                                         (str " url(#"
                                              (-> commit :author :md5)
                                              ")"))}]]
                   (->> parent-ids
                        (map
                         (fn path-drawer [parent-id]
                           (let [parent (get commits-map parent-id)
                                 parent-column (get column-map parent-id)
                                 path-color (if (= first-parent-id parent-id)
                                              commit-color
                                              (color parent-column head-col))
                                 parent-at (get reverse-index parent-id
                                                canvas-em-height)
                                 parent-x (+ 16 (* 32 parent-column))
                                 parent-y (+ 16 (* 32 parent-at))]
                             (if (= parent-column column)
                               [:path
                                {:key (gensym)
                                 :stroke path-color
                                 :stroke-width 2.5
                                 :fill "none"
                                 :d (join " "
                                      [(join " " ["M" commit-x commit-y])
                                       (join " " ["L" parent-x parent-y])])}]
                               [:path
                                {:key (gensym)
                                 :stroke path-color
                                 :stroke-width 2.5
                                 :fill "none"
                                 :d (if (< parent-x commit-x)
                                      ;; left side
                                      (if (and merge?
                                               (not= parent-id first-parent-id))
                                        (join
                                         " "
                                         [(join " " ["M" commit-x commit-y])
                                          (join " " ["L" (+ 16 parent-x) commit-y])
                                          (join " " ["A" 16 16 0 0 0
                                                         parent-x (+ 16 commit-y)])
                                          (join " " ["L" parent-x parent-y])])
                                        (join
                                         " "
                                         [(join " " ["M" commit-x commit-y])
                                          (join " " ["L" commit-x (- parent-y 16)])
                                          (join " " ["A" 16 16 0 0 1
                                                         (- commit-x 16) parent-y])
                                          (join " " ["L" parent-x parent-y])]))
                                      ;; right side
                                      (if (and merge?
                                               (not= parent-id first-parent-id))
                                        (join
                                         " "
                                         [(join " " ["M" commit-x commit-y])
                                          (join " " ["L" (- parent-x 16) commit-y])
                                          (join " " ["A" 16 16 0 0 1
                                                         parent-x (+ 16 commit-y)])
                                          (join " " ["L" parent-x parent-y])])
                                        (join
                                         " "
                                         [(join " " ["M" commit-x commit-y])
                                          (join " " ["L" commit-x (- parent-y 16)])
                                          (join " " ["A" 16 16 0 0 0
                                                         (+ 16 commit-x) parent-y])
                                          (join " " ["L" parent-x parent-y])])))}]))))
                        (append circle)))))
              (into svg-header))))
     (->> commits
        (map
         (fn commit-to-element
           [commit]
           (let [relevant-branches (get indexed-branches (.-id commit))]
             [:li
              {:key (gensym)}
              (some->> relevant-branches
                       (map (fn [branch]
                              [:span
                               {:key (gensym)
                                :class ["branch-label"
                                        (when (.-isHead branch)
                                          "head")]}
                               (-> (.-name branch)
                                   (split #"/")
                                   (last))])))
              [:span
               {:key (gensym)
                :class ["message"]
                :title (.-id commit)}
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
