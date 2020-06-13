(ns electron.core
  (:require [electron.events :as events]
            ["electron" :refer [app BrowserWindow crashReporter ipcMain]]))

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                        (clj->js {:fullscreenable false ;; the f11 fullscreen
                                  :webPreferences
                                  {:preload (str js/__dirname "/preload.js")
                                   :nodeIntegration true}})))
  ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on @main-window "closed" #(reset! main-window nil)))

(defn main []
  ; CrashReporter can just be omitted
  (.start crashReporter
          (clj->js
            {:companyName "MyAwesomeCompany"
             :productName "MyAwesomeApp"
             :submitURL "https://example.com/submit-url"
             :autoSubmit false}))

  (set! (.. app -allowRendererProcessReuse) false)
  (.on ipcMain "ipc-request" events/ipc-handler)
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser))
