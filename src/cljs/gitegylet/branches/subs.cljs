(ns gitegylet.branches.subs
  (:require [re-frame.core :as rf]
            [clojure.string :refer [join split]]
            [gitegylet.branches.db :refer [branch->map]]
            [gitegylet.git :refer [git]]))
