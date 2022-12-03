(defproject functionalbytes/mount-lite "2.3.0"
  :description "mount, but different and light"
  :url "https://github.com/aroemers/mount-lite"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:test {:dependencies [[org.clojure/tools.namespace "0.2.11"]]}
             :tools-namespace-0.3.x {:dependencies [^:replace [org.clojure/tools.namespace "0.3.1"]]}
             :tools-namespace-1.x.x {:dependencies [^:replace [org.clojure/tools.namespace "1.1.0"]]}}
  :global-vars {*warn-on-reflection* true})
