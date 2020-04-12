(ns mount.lite
  "The core namespace providing the public API"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false})

;;; Internals

(defonce ^:dynamic *system-key* :default)
(defonce ^:dynamic *substitutes* {})
(defonce ^:dynamic *system* {})

(defonce systems  (atom {}))
(defonce stoppers (atom {}))
(defonce states   (atom {}))

(defprotocol IState
  (start* [this])
  (stop* [this])
  (status* [this]))

(defrecord StateVar [name order]
  IState
  (start* [this]
    (when (= :stopped (status* this))
      (let [state  (or (get *substitutes* this) (get @states this))
            result (start* state)]
        (swap! systems  update *system-key* assoc this result)
        (swap! stoppers update *system-key* assoc this state))))

  (stop* [this]
    (when (= :started (status* this))
      (when-let [state (get-in @stoppers [*system-key* this])]
        (stop* state))
      (swap! systems  update *system-key* dissoc this)
      (swap! stoppers update *system-key* dissoc this)))

  (status* [this]
    (if (or (contains? *system* this)
            (contains? (get @systems *system-key*) this))
      :started
      :stopped))

  clojure.lang.IDeref
  (deref [this]
    (if (= :started (status* this))
      (if (contains? *system* this)
        (get *system* this)
        (get-in @systems [*system-key* this]))
      (throw (ex-info (str "Cannot deref state " name " when not started (system " *system-key* ")")
                      {:state this :system *system-key*})))))

(defrecord State [start-fn stop-fn]
  IState
  (start* [_] (start-fn))
  (stop*  [_] (stop-fn)))

(prefer-method print-method clojure.lang.IRecord clojure.lang.IDeref)


;;; Core public API

(defmacro state
  "Create an anonymous state, useful for substituting. Takes a :start
  and a :stop expression."
  [& {:keys [start stop]}]
  `(State. (fn [] ~start) (fn [] ~stop)))

(defmacro defstate
  "Define a global state. Takes a :start and a :stop expression.
  Redefining a defstate does not affect the stop logic of an already
  started defstate."
  [name & {:keys [start stop]}]
  `(let [var#       (or (defonce ~name (StateVar. ~(str *ns* "/" name) (clojure.lang.RT/nextID)))
                        (resolve '~name))
         state-var# (deref var#)
         state#     (state :start ~start :stop ~stop)]
     (swap! states assoc state-var# state#)
     var#))

(defn start
  "Starts all the unstarted global defstates, in the context of the
  current system key. Takes an optional state, starting the system
  only up to that particular state."
  ([] (start nil))
  ([up-to]
   (let [ordered        (sort-by :order (keys @states))
         [before after] (split-with (complement #{up-to}) ordered)
         state-vars     (concat before (take 1 after))]
     (doseq [state-var state-vars]
       (start* state-var)))))

(defn stop
  "Stops all the started global defstates, in the context of the current
  system key. Takes an optional state, stopping the system
  only up to that particular state."
  ([] (stop nil))
  ([up-to]
   (let [ordered        (sort-by (comp - :order) (keys @states))
         [before after] (split-with (complement #{up-to}) ordered)
         state-vars     (concat before (take 1 after))]
     (doseq [state-var state-vars]
       (stop* state-var)))))

(defn status
  "Returns a status map of all the states."
  []
  (reduce #(assoc %1 (:name %2) (status* %2)) {} (keys @states)))


;;; Advanced public API

(defmacro with-substitutes
  "Executes the given body while the given defstates' start/stop logic
  have been substituted. These can be nested."
  [substitutes & body]
  `(binding [*substitutes* (merge *substitutes* ~substitutes)]
     ~@body))

(defmacro with-system-key
  "Executes the given body in the context of the given system key, like
  starting/stopping a system or dereferencing a defstate in that
  system. This allows multiple parallel state systems."
  [key & body]
  `(binding [*system-key* ~key]
     ~@body))

(defmacro with-system-map
  "Executes the given body while the given system map has been merged in
  the (possibly empty) existing system. These can be nested."
  [system & body]
  `(binding [*system* (merge *system* ~system)]
     ~@body))
