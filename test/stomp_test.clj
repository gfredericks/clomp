(ns stomp-test
  (:use clojure.test)
  (:require stomp)
  (:import [java.net Socket]))

(deftest connect-disconnect
  (let [s (java.net.Socket. "localhost" 61613)]
    (let [connected (stomp/connect s {:login "foo"})]
      (is (= :CONNECTED (:type connected)))
      (is (get-in connected [:headers :session]))
      (is (= "" (:body connected))))
    (stomp/disconnect s)))

(deftest simple-session
  (let [s (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (is stomp/*session-id*)
      (is stomp/*connection*)
      (stomp/subscribe s {:destination "/queue/a" :ack "auto"})
      (stomp/send s {:destination "/queue/a"} "blah")
      (stomp/send s {:destination "/queue/a"} "blah?")
      (stomp/send s {:destination "/queue/a"} "blah!")
      (let [received (stomp/receive s)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/a" (get-in received [:headers :destination])))
        (is (= "blah" (:body received))))
      (is (= "blah?" (:body (stomp/receive s))))
      (is (= "blah!" (:body (stomp/receive s)))))))

(deftest two-clients
  (let [s1 (java.net.Socket. "localhost" 61613)
        s2 (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s1 {:login "foo" :password "secret"}
      (stomp/send s1 {:destination "/queue/a"} "zap!")
      (stomp/send s1 {:destination "/queue/a"} "baz!"))
    (stomp/with-connection s2 {:login "baz" :password "password"}
      (stomp/subscribe s2 {:destination "/queue/a"})
      (is (= "zap!" (:body (stomp/receive s2))))
      (is (= "baz!" (:body (stomp/receive s2)))))))

(deftest three-clients
  (let [s1 (java.net.Socket. "localhost" 61613)
        s2 (java.net.Socket. "localhost" 61613)
        s3 (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s1 {:login "foo" :password "secret"}
      (stomp/send s1 {:destination "/queue/a"} "1")
      (stomp/send s1 {:destination "/queue/a"} "2")
      (stomp/send s1 {:destination "/queue/a"} "3")
      (stomp/send s1 {:destination "/queue/a"} "4"))
    (stomp/with-connection s2 {:login "baz" :password "password"}
      (stomp/subscribe s2 {:destination "/queue/a"})
      (is (= "1" (:body (stomp/receive s2))))
      (is (= "2" (:body (stomp/receive s2)))))
    (stomp/with-connection s3 {:login "baz" :password "password"}
      (stomp/subscribe s3 {:destination "/queue/a"})
      (is (= "3" (:body (stomp/receive s3))))
      (is (= "4" (:body (stomp/receive s3)))))))
