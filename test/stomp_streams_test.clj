(ns stomp-streams-test
  (:use clojure.test
        [clojure.java.io :only [reader writer]])
  (:require stomp)
  (:import [java.net Socket]))

(deftest outstream
  (with-open [s   (java.net.Socket. "localhost" 61613)
              out (stomp/outstream s {:destination "/queue/a"})]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (stomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (writer out)]
        (print "cmb")
        (flush))
      (let [received (stomp/receive s)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/a" (get-in received [:headers :destination])))
        (is (= "cmb" (:body received)))))))

(deftest outstream-eof
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (stomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (stomp/writer s {:destination "/queue/a"})]
        (print "cmb")
        (flush)
        (.close *out*))
      (is (= "cmb" (:body (stomp/receive s))))
      (is (get-in (stomp/receive s) [:headers :eof])))))

(deftest instream
  (with-open [s  (java.net.Socket. "localhost" 61613)
              in (stomp/instream s)]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (stomp/subscribe s {:destination "/queue/a"})
      (stomp/send s {:destination "/queue/a"} "hrm\n")
      (stomp/send s {:destination "/queue/a"} "jlb\n")
      (stomp/send s {:destination "/queue/a"} "cmb\n")
      (binding [*in* (reader in)]
        (is (= "hrm" (read-line)))
        (is (= "jlb" (read-line)))
        (is (= "cmb" (read-line)))))))

(deftest instream-eof
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (stomp/subscribe s {:destination "/queue/a"})
      (stomp/send s {:destination "/queue/a"} "count to thirteen")
      (stomp/send s {:destination "/queue/a" :eof true} "")
      (binding [*in* (stomp/reader s)]
        (is (= "count to thirteen" (read-line)))
        (is (= nil (read-line)))))))

(deftest instream-outstream
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (stomp/with-connection s {:login "foo" :password "secret"}
      (stomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (stomp/writer s {:destination "/queue/a"})]
        (println "foo")
        (println "bar")
        (println "baz")
        (.close *out*))
      (binding [*in* (stomp/reader s)]
        (is (= "foo" (read-line)))
        (is (= "bar" (read-line)))
        (is (= "baz" (read-line)))))))
