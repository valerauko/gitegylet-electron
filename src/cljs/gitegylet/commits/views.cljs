(ns gitegylet.commits.views
  (:require [re-frame.core :as rf]
            [clojure.string :refer [split join]]
            [gitegylet.commits.subs :as subs]
            [gitegylet.branches.subs :as branches]
            [gitegylet.commits.db :refer [commit->map]]))

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
   (let [hue (rem (+ 50 (* (- i head) 45)) 360)]
     (str "hsl(" hue ", 70%, 45%)"))))

(defn append [item ls] (concat ls (list item)))

(defn commits
  []
  (let [commits @(rf/subscribe [::subs/commits])
        head @(rf/subscribe [::subs/head])
        branches @(rf/subscribe [::branches/selected])
        indexed-branches (group-by :commit-id branches)]
    [:div {:class "commits"}
     (let [ordered-ids (into [] (map :id) commits)
           reverse-index (->> ordered-ids
                              (map-indexed (fn [i v] [v i]))
                              (into {}))
           commits-map (index-by-id　commits)
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
           svg-header [:svg {:style
                             {:width (str (* 2 column-count) "em")
                              :height (str canvas-em-height "em")
                              :transform "scale(-1,1)"}}
                       icons]]
       (->> ordered-ids
            (map-indexed
             (fn commit-drawer [idx id]
               (let [commit (get commits-map id)
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
                               :fill (if merge?
                                       commit-color
                                       (str " url(#"
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
     (into [:ol]
       (map
        (fn commit-to-element
          [commit]
          (let [relevant-branches (get indexed-branches (:id commit))]
            [:li
             {:key (gensym)}
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
           commits)]))
