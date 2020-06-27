(ns gitegylet.branches.db)

(defn branch->map
  [branch]
  {:full-name (.-name branch)
   :commit-id (.-commitId branch)
   :head? (.-isHead branch)
   :ahead-behind (.-aheadBehind branch)})
