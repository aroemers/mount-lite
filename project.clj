(defproject functionalbytes/mount-lite "0.9.3"
  :description "Mount, but Clojure only and a more flexible API."
  :url "https://github.com/aroemers/mount-lite"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.namespace "0.3.0-alpha3"]
                 [org.clojure/java.classpath "0.2.3"]
                 [com.stuartsierra/dependency "0.2.0"]]
  :plugins [[lein-codox "0.9.1"]]
  :codox {:output-path "../mount-lite-gh-pages"})
