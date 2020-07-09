(ns gitegylet.menu)

(defonce remote
  (.-remote (js/require "electron")))

(defn build
  [template]
  (.buildFromTemplate
   (.-Menu remote)
   (clj->js template)))
