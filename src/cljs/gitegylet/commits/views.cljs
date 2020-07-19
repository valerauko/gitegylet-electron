(ns gitegylet.commits.views
  (:require [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            [gitegylet.menu :as menu]
            [gitegylet.commits.subs :as subs]
            [gitegylet.commits.events :as events]
            [gitegylet.repo.subs :as repo]
            [gitegylet.branches.subs :as branches]
            [gitegylet.branches.views :refer [create-branch-modal]]
            [gitegylet.commits.db :refer [commit->map]]))

(defn modified-tree
  [statuses]
  [:ol
   (map
    (fn [{:keys [file status]}]
      [:li {:key (gensym)} (str file ": " status)])
    statuses)])

(defn selected-commit-pane
  []
  [:div
   {:id "selected-commit-pane"}
   (if-let [id @(rf/subscribe [::subs/selected])]
     (let [;; FIXME: this needs to be optimized
           commit (first (filter #(= (:id %) id) @(rf/subscribe [::subs/commits])))
           statuses @(rf/subscribe [::subs/diff-files id])]
       [:div
        [:p
         (:summary commit)
         [:br]
         (-> commit :message (split #"[\n\r]+") rest)
         [:br]
         (:id commit)]
        [:p
         (:name (:author commit))
         [:br]
         (->> (:time commit)
              (* 1000)
              (new js/Date)
              (.toISOString))]
        (modified-tree statuses)])
     (let [statuses @(rf/subscribe [::repo/statuses])]
       (modified-tree statuses)))])

(defn index-by-id
  [commits]
  (reduce
   (fn id-indexer
     [aggr commit]
     (let [commit-id (:id commit)]
       (assoc aggr commit-id commit)))
   {}
   commits))

(defn color
  ([i] (color i -1))
  ([i head]
   (let [bs (Math/abs (- (rem i 5) head))
         v (- 50 (* bs 10))]
     (str "hsl(45, 100%, " v "%)"))))

(defn append [item ls] (concat ls (list item)))

(defn commit-menu
  [event]
  (let [commit-id (-> event .-target .-id)]
    (-> [{:id "createBranch"
          :label "Create &branch here"
          :click #(rf/dispatch [:gitegylet.modal.events/show
                                (create-branch-modal commit-id)])}]
        (menu/build)
        (.popup))))

(defn commits
  []
  (let [commits @(rf/subscribe [::subs/commits])
        head @(rf/subscribe [::subs/head])
        selected-id @(rf/subscribe [::subs/selected])
        statuses @(rf/subscribe [::repo/statuses])
        branches @(rf/subscribe [::branches/selected])
        indexed-branches (group-by :commit-id branches)]
    [:div {:class "commits"}
     (let [ordered-ids (into (if (empty? statuses) [] [:dirty])
                             (map :id) commits)
           reverse-index (->> ordered-ids
                              (map-indexed (fn [i v] [v i]))
                              (into {}))
           commits-map (let [indexed (index-by-id commits)]
                         (if (empty? statuses)
                           indexed
                           (assoc indexed
                                  :dirty
                                  (let [head-id (:id head)]
                                    {:id :dirty
                                     :parents [head-id]
                                     :column (-> head-id
                                                 indexed
                                                 :column)}))))
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
                      (into [:defs]))
           column-count (->> commits (map :column) (apply max) (inc))
           head-col (->> head (:id) (commits-map) (:column))
           canvas-em-height (* 2 (count ordered-ids))
           svg-header (-> [:svg {:style
                                 {:width (str (* 2 column-count) "em")
                                  :height (str canvas-em-height "em")
                                  :transform "scale(-1,1)"}}]
                          (into icons))]
       (->> ordered-ids
            (map-indexed
             (fn commit-drawer [idx id]
               (let [dirty? (= id :dirty)
                     commit (get commits-map id)
                     parent-ids (:parents commit)
                     first-parent-id (first parent-ids)
                     merge? (> (count parent-ids) 1)
                     column (:column commit)
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
                               :style (if dirty? {:stroke-dasharray 4})
                               :fill (cond
                                       merge? commit-color
                                       dirty? "#1a1d21"
                                       :else (str " url(#"
                                                  (-> commit :author :md5)
                                                  ")"))}]]
                 (->> parent-ids
                      (map
                       (fn path-drawer [parent-id]
                         (let [parent (get commits-map parent-id)
                               parent-column (:column parent)
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
                               :style (if dirty? {:stroke-dasharray 4})
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
            (into svg-header)))
     (-> [:ol]
         (into (if (empty? statuses)
                 []
                 [[:li
                   {:key (gensym)
                    :on-click #(rf/dispatch [::events/toggle-select])}
                   (->> statuses
                        (reduce
                         (fn [aggr {:keys [status]}]
                           (update aggr status inc))
                         {})
                        (map
                         (fn [[status files]]
                           [:span {:key (gensym)}
                            [:span
                             {:alt status
                              :class "status"}
                             (case status
                               "modified" "\uf304"
                               "new" "\uf0fe"
                               "deleted" "\uf146")]
                            (str " " files)])))]]))
         (into (map
                (fn commit-to-element
                  [commit]
                  (let [relevant-branches (get indexed-branches (:id commit))]
                    [:li
                     {:key (gensym)
                      :on-context-menu commit-menu
                      :on-click #(rf/dispatch [::events/toggle-select
                                               (:id commit)])
                      :class [(when (= (:id commit) selected-id)
                                "selected")]}
                     (some->> relevant-branches
                              (map (fn [branch]
                                     [:span
                                      {:key (gensym)
                                       :class ["branch-label"
                                               (when (:head? branch)
                                                 "head")]}
                                      (-> (:full-name branch)
                                          (split #"/")
                                          (last))])))
                     [:span
                      {:key (gensym)
                       :id (:id commit)
                       :class ["message"]
                       :title (:id commit)}
                      (:summary commit)]])))
               commits))]))
