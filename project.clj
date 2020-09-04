(defproject functionalbytes/mount-lite "2.1.5"
  :description "mount, but different and light"
  :url "https://github.com/aroemers/mount-lite"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:test {:dependencies [[org.clojure/tools.namespace "0.2.11"]]}}
  :global-vars {*warn-on-reflection* true})
