(ns mount.housekeeping
  "Housekeeping functions."
  (:require [mount.implementation.statevar :as impl]))

;;; Public API

(defn forget!
  "Forget all known global states. This can sometimes be necessary to
  have mount-lite pick up on new and removed global states in the
  correct order as they are reloaded again.

  You will have to do this reloading yourself, using `(require
  'my-app.core :reload-all)` for example. No system(s) can be running
  when calling reset!"
  []
  (impl/forget!))

(defn reload!
  "Combines forget! with a best effort of reloading your application
  based on the currently known global states."
  []
  (impl/reload!))
