(ns gitegylet.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [gitegylet.config :as config]
            [gitegylet.views :as views]
            [gitegylet.events :as events]))

(set! *warn-on-infer* true)

; receive ipc message from main
; sent from electron/events.cljs
; forwarded by resources/preload.js
(.addEventListener js/window "message"
  (fn ipc-handler [event]
    (if (= (-> event .-data .-type) "ipc-response")
      (let [message (-> event .-data .-payload)
            handler (keyword 'gitegylet.events (.-type message))
            ; converting js objects to cljs maps is a pain in the ass
            payload (-> message .-payload ,,,)]
        (rf/dispatch [handler payload])))))

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
  (dev-setup)
  (mount-root))
