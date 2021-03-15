(ns mount.extensions.autostart
  (:require [mount.lite :as mount])
  (:import [clojure.lang IDeref]
           [mount.lite IState]))

(defonce
  ^{:doc "The fn that will be called to automatically start any state
    that has been deref'd but not started. If this fn is unsuccessful
    in starting the state, the standard error handling will occur."}
  autostart-fn mount/start)

(defn set-autostart-fn!
  "Configures `autostart-fn` use by `AutoStartState` during state deref."
  [fn]
  (alter-var-root #'autostart-fn (constantly fn)))

(defrecord AutoStartState [var state]
  IState
  (start* [_]
    (mount/start* state))

  (stop* [_]
    (mount/stop* state))

  (status* [_]
    (mount/status* state))

  (properties [_]
    (mount/properties state))

  IDeref
  (deref [_]
    (if (= :started (mount/status* state))
      @state
      ;; synchronize access across thread boundaries just in case
      ;; two threads want to access/start this state at the same time.
      (locking var
        (autostart-fn var)
        @state))))

(defmacro defstate
  "Defines a state that will be auto-started on first deref, including its dependencies.

  See `mount.lite/defstate` for more information."
  [name & args]
  `(do
     (mount/defstate ~name ~@args)
     (alter-var-root (var ~name) (fn [state#] (->AutoStartState (var ~name) state#)))
     (var ~name)))
