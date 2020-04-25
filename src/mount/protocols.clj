(ns mount.protocols
  "This namespace declares the core protocols. Mostly an implementation
  detail, but can be used to supply your own state implementations for
  substitutes."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false})

(defprotocol IState
  :extend-via-metadata true
  (start [this])
  (stop [this]))

(defprotocol IStatus
  :extend-via-metadata true
  (status [this]))
