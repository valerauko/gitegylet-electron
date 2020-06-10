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

(defn dev-setup []
  (when config/debug?
    (devtools/install!)
    (enable-console-print!)))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [views/ui]
                  (js/document.getElementById "app")))

(init)
