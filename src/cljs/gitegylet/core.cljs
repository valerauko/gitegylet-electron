(ns gitegylet.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [gitegylet.config :as config]
            [gitegylet.views :as views]
            [gitegylet.events :as events]))

(set! *warn-on-infer* true)

(defn dev-setup []
  (when config/debug?
    (devtools/install!)
    (enable-console-print!)))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (dom/unmount-component-at-node root-el)
    (dom/render [views/ui] root-el)))

(defn ^:export init
  []
  (rf/dispatch-sync [::events/initialize-db])
  (rf/dispatch [::events/reload-branches])
  (dev-setup)
  (mount-root))
