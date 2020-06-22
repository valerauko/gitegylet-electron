(ns gitegylet.db
  (:require ; [cljs.spec.alpha :as s]
            [cljs.reader :refer [read-string]]
            [re-frame.core :as rf]))

;; TODO: add schema specs

(def local-storage-key
  "gitegylet-renderer")

(defn persist
  "Persists DB to localStorage"
  [db]
  (.setItem js/localStorage local-storage-key (str db)))

(rf/reg-cofx
  ::persistence
  (fn [state _]
    (assoc state :persisted
      (some->> local-storage-key
               (.getItem js/localStorage)
               (read-string)))))
