(ns stomp-test
  (:use clojure.test)
  (:require stomp)
  (:import [java.net Socket]))

(deftest simple-session
  (let [s (java.net.Socket. "localhost" 61613)]
    (let [connected (stomp/connect s {})]
      (is (= :CONNECTED (:type connected)))
      (is (get-in connected [:headers :session]))
      (is (= "" (:body connected))))
    (stomp/subscribe s {:destination "/queue/foo"})
    (stomp/send s {:destination "/queue/foo"} "blah")
    (let [received (stomp/receive s)]
      (is (= :MESSAGE (:type received)))
      (is (= "/queue/foo" (get-in received [:headers :destination])))
      (is (= "blah" (:body received))))
    (stomp/disconnect s)))
