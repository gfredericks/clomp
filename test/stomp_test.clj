(ns stomp-test
  (:use clojure.test)
  (:require stomp)
  (:import [java.net Socket]))

(deftest connect-disconnect
  (let [s (java.net.Socket. "localhost" 61613)]
    (let [connected (stomp/connect s {})]
      (is (= :CONNECTED (:type connected)))
      (is (get-in connected [:headers :session]))
      (is (= "" (:body connected))))
    (stomp/disconnect s)))

(deftest simple-session
  (let [s (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s {}
      (stomp/subscribe s {:destination "/queue/foo"})
      (stomp/send s {:destination "/queue/foo"} "blah")
      (let [received (stomp/receive s)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/foo" (get-in received [:headers :destination])))
        (is (= "blah" (:body received)))))))

(deftest two-clients
  (let [s1 (java.net.Socket. "localhost" 61613)
        s2 (stomp/clone s1)]
    (stomp/with-connection s1 {}
      (stomp/send s1 {:destination "/queue/foo"} "zap!"))
    (stomp/with-connection s2 {}
      (stomp/subscribe s2 {:destination "/queue/foo"})
      (let [received (stomp/receive s2)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/foo" (get-in received [:headers :destination])))
        (is (= "zap!" (:body received)))))))