(ns gitegylet.commits.db)

(defn commit->map
  [commit]
  (let [parents (js->clj (.-parents commit))
        commit-id (.-id commit)]
    {:id commit-id
     :author
     (let [author (.-author commit)]
       {:name (.-name author)
        :email (.-email author)
        :md5 (.-md5 author)})
     :time (.-timestamp commit)
     :parents parents
     :message (.-message commit)
     :summary (.-summary commit)
     :column (.-column commit)}))
