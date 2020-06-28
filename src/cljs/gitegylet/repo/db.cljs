(ns gitegylet.repo.db)

(defn status->map
  [item]
  {:file (.-file item)
   :status (.-status item)})
