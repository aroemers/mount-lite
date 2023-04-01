(ns mount.extensions.common-deps-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [mount.extensions.common-deps :as sut]))

(def system "system")

(def graphs
  {:dependencies {::overrides   #{::overrides-a ::overrides-b}
                  ::config-a    #{}
                  ::overrides-a #{::config-a}
                  ::config-b    #{}
                  ::registry    #{}
                  ::config      #{::overrides ::env}
                  ::system      #{::config ::debug ::db}
                  ::debug       #{}
                  ::db          #{::config}
                  ::env         #{}
                  ::overrides-b #{::config-b}}
   :dependents   {::overrides   #{::config}
                  ::config-a    #{::overrides-a}
                  ::overrides-a #{::overrides}
                  ::config-b    #{::overrides-b}
                  ::registry    #{}
                  ::config      #{::system ::db}
                  ::system      #{}
                  ::debug       #{::system}
                  ::db          #{::system}
                  ::env         #{::config}
                  ::overrides-b #{::overrides}}})

(def states
  [::overrides
   ::config-a
   ::overrides-a
   ::config-b
   ::registry
   ::config
   ::system
   ::debug
   ::db
   ::env
   ::overrides-b])

(deftest transitives-test
  (testing "ordering active states in dependency order"
    (is (= [::config-a
            ::overrides-a
            ::config-b
            ::overrides-b
            ::overrides
            ::env
            ::config
            ::db
            ::debug
            ::system]
           (sut/transitives #'system graphs states)))
    (is (= [::config-a
            ::overrides-a
            ::config-b
            ::overrides-b
            ::overrides
            ::env
            ::config
            ::db
            ::debug
            ::system
            ::registry]
           (sut/transitives nil graphs states)))))
