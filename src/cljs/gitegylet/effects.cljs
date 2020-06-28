(ns gitegylet.effects
  {:reference "https://day8.github.io/re-frame/FAQs/PollADatabaseEvery60/"}
  (:require [re-frame.core :as rf]))

(def interval-handler
  (let [live-intervals (atom {})]
    (fn handler [{:keys [action id freq event]}]
      (condp = action
        :clean (mapv #(handler {:action :end :id %})
                     (keys @live-intervals))
        :start (let [interval-id (js/setInterval #(rf/dispatch event) freq)]
                 (swap! live-intervals assoc id interval-id))
        :end   (do
                 (js/clearInterval (get @live-intervals id))
                 (swap! live-intervals dissoc id))))))

(interval-handler {:action :clean})

(re-frame.core/reg-fx
  ::interval
  interval-handler)
