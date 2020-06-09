(ns gitegylet.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [devtools.core :as devtools]
            [gitegylet.views :refer [ui]]
            [gitegylet.subs]
            [gitegylet.events]
            [gitegylet.db]
            ))

(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [gitegylet.views/ui]
                  (js/document.getElementById "app-container")))

(init)
