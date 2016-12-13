(defproject functionalbytes/mount-lite "2.0.0-SNAPSHOT"
  :description "mount, but different and light"
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
