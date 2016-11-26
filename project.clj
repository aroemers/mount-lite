(defproject functionalbytes/mount-lite "0.10.0-SNAPSHOT"
  :description "Mount, but Clojure only and a more flexible API."
  :url "https://github.com/aroemers/mount-lite"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:uberjar {:source-paths ["src" "test-aot"]
                       :aot [mount.aot-test mount.lite]
                       :main mount.aot-test
                       :omit-source true
                       :uberjar-name "mount-lite-standalone.jar"}}
  :plugins [[lein-codox "0.9.1"]]
  :codox {:output-path "../mount-lite-gh-pages"})
