(ns gitegylet.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [gitegylet.views :as views]
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
  (reagent/render [views/ui]
                  (js/document.getElementById "app")))

(init)
