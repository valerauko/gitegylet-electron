(ns gitegylet.subs
  (:require
   [clojure.string :refer [join split]]
   [re-frame.core :as rf]
   [gitegylet.git :refer [git]]))

(rf/reg-sub
  ::repo
  (fn [db _]
    (:repo db)))
