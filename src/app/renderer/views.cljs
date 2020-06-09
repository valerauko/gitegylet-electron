(ns app.renderer.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [clojure.string :as str]))

(def hello (js/require "@amilajack/neon-hello"))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn clock
  []
  [:div.example-clock
   {:style {:color @(rf/subscribe [:time-color])}}
   (-> @(rf/subscribe [:time])
       .toTimeString
       (str/split " ")
       first)])

(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @(rf/subscribe [:time-color])
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])  ;; <---

(defn ui
  []
  [:div
   [:h1 "Hello world, it is now"]
   [:p (.helloWorld hello)]
   [clock]
   [color-input]])
