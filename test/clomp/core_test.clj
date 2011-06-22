(ns clomp.core-test
  (:use clojure.test)
  (:require [clomp.core :as clomp])
  (:import [java.net Socket]))

(deftest connect-disconnect
  (let [s (java.net.Socket. "localhost" 61613)]
    (let [connected (clomp/connect s {:login "foo"})]
      (is (= :CONNECTED (:type connected)))
      (is (get-in connected [:headers :session]))
      (is (= "" (:body connected))))
    (clomp/disconnect s)))

(deftest simple-session
  (let [s (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (is clomp/*session-id*)
      (is clomp/*connection*)
      (clomp/subscribe s {:destination "/queue/a" :ack "auto"})
      (clomp/send s {:destination "/queue/a"} "blah")
      (clomp/send s {:destination "/queue/a"} "blah?")
      (clomp/send s {:destination "/queue/a"} "blah!")
      (let [received (clomp/receive s)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/a" (get-in received [:headers :destination])))
        (is (= "blah" (:body received))))
      (is (= "blah?" (:body (clomp/receive s))))
      (is (= "blah!" (:body (clomp/receive s)))))))

(deftest two-clients
  (let [s1 (java.net.Socket. "localhost" 61613)
        s2 (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s1 {:login "foo" :password "secret"}
      (clomp/send s1 {:destination "/queue/a"} "zap!")
      (clomp/send s1 {:destination "/queue/a"} "baz!"))
    (clomp/with-connection s2 {:login "baz" :password "password"}
      (clomp/subscribe s2 {:destination "/queue/a"})
      (is (= "zap!" (:body (clomp/receive s2))))
      (is (= "baz!" (:body (clomp/receive s2)))))))

(deftest three-clients
  (let [s1 (java.net.Socket. "localhost" 61613)
        s2 (java.net.Socket. "localhost" 61613)
        s3 (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s1 {:login "foo" :password "secret"}
      (clomp/send s1 {:destination "/queue/a"} "1")
      (clomp/send s1 {:destination "/queue/a"} "2")
      (clomp/send s1 {:destination "/queue/a"} "3")
      (clomp/send s1 {:destination "/queue/a"} "4"))
    (clomp/with-connection s2 {:login "baz" :password "password"}
      (clomp/subscribe s2 {:destination "/queue/a"})
      (is (= "1" (:body (clomp/receive s2))))
      (is (= "2" (:body (clomp/receive s2)))))
    (clomp/with-connection s3 {:login "baz" :password "password"}
      (clomp/subscribe s3 {:destination "/queue/a"})
      (is (= "3" (:body (clomp/receive s3))))
      (is (= "4" (:body (clomp/receive s3)))))))
