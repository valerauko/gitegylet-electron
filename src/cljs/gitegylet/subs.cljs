(ns gitegylet.subs
  (:require
   [clojure.string :refer [join split]]
   [re-frame.core :as rf]
   [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::-branches-selected
  (fn [db _]
    (:branches-selected db)))

(rf/reg-sub
  ::-folders-expanded
  (fn [db _]
    (:folders-expanded db)))

(rf/reg-sub
  ::repo
  (fn [db _]
    (:repo db)))

(rf/reg-sub
  ::branches
  :<- [::repo]
  (fn [repo-path _]
    (.localBranches git repo-path)))

(rf/reg-sub
  ::branch-names
  :<- [::branches]
  (fn [branches _]
    (map #(.-name %) branches)))

(rf/reg-sub
  ::branches-selected
  :<- [::branches]
  :<- [::-branches-selected]
  (fn [[branches selected] _]
    (let [selected-filter (if (empty? selected)
                            (constantly true)
                            (into #{} selected))]
      (filter #(some-> % (.-name) (selected-filter)) branches))))

(rf/reg-sub
  ; use the branches-selected key in the db if present else select every branch
  ::branch-names-selected
  :<- [::branch-names]
  :<- [::-branches-selected]
  (fn [[branches selected] _]
    (or selected branches)))

(rf/reg-sub
  ; use the folders-expanded key in the db if present else expand every folder
  ::folders-expanded
  :<- [::branch-names]
  :<- [::-folders-expanded]
  (fn [[branches expanded] _]
    (if expanded
      expanded
      (->> branches
           (map (fn [name]
                  (-> name
                      (split #"/")
                      (butlast))))
           (filter (complement empty?))
           (map #(join "/" %))))))

(rf/reg-sub
  ::commits
  :<- [::repo]
  :<- [::branch-names-selected]
  (fn [[repo-path branch-names] _]
    (when-not (empty? branch-names)
      (.commits git repo-path (clj->js branch-names)))))
