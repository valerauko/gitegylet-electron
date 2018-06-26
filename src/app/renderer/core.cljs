(ns app.renderer.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]  
            ["nedb" :as nedb]
            [devtools.core :as devtools] 
            [app.renderer.views :refer [ui]]
            [app.renderer.subs]
            [app.renderer.events]
            [app.renderer.db]
            ))

(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])  
  (reagent/render [app.renderer.views/ui]           
                  (js/document.getElementById "app-container")))

(init)
