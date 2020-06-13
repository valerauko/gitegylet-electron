(ns gitegylet.db
  (:require [cljs.spec.alpha :as s]
            [cljs.reader :refer [read-string]]
            [re-frame.core :as rf]))

;; TODO: add schema specs

(defn ->local-storage
  "Persists DB to localStorage"
  [db]
  (.setItem js/localStorage "gitegylet" (str db)))

(rf/reg-cofx
 ::local-storage
 (fn [state _]
   (assoc state :local-storage
     (some->> "gitegylet"
              (.getItem js/localStorage)
              (read-string)))))
