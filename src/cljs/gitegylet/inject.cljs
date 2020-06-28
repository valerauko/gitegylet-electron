(ns gitegylet.inject
  {:reference "https://github.com/den1k/re-frame-utils/blob/a3f4d6c/src/vimsical/re_frame/cofx/inject.cljc"}
  (:require [re-frame.core :as rf]
            [re-frame.interop :as interop]))

(defn- ignore-dispose?
  [query-vector]
  (:ignore-dispose (meta query-vector)))

(defn- dispose-maybe
  "Dispose of `ratom-or-reaction` iff it has no watches."
  [query-vector ratom-or-reaction]
  (when-not (seq (.-watches ratom-or-reaction))
    (when-not (ignore-dispose? query-vector)
      (js/console :warn "Disposing of injected subscription:" query-vector))
    (interop/dispose! ratom-or-reaction)))

(rf/reg-cofx
  ::sub
  (fn [cofx [id :as qv]]
    (let [sub (rf/subscribe qv)
          value (deref sub)]
      (dispose-maybe qv sub)
      (assoc cofx id value))))
