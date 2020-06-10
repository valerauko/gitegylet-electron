(ns gitegylet.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [gitegylet.config :as config]
            [gitegylet.views :as views]
            [gitegylet.subs]
            [gitegylet.events]
            [gitegylet.db]
            ))

(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [views/ui]
                  (js/document.getElementById "app")))

(init)
