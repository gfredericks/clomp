(ns clomp.streams-test
  (:use clojure.test
        [clojure.java.io :only [reader writer]])
  (:require [clomp.core :as clomp])
  (:import [java.net Socket]))

(deftest outstream
  (with-open [s   (java.net.Socket. "localhost" 61613)
              out (clomp/outstream s {:destination "/queue/a"})]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (clomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (writer out)]
        (print "cmb")
        (flush))
      (let [received (clomp/receive s)]
        (is (= :MESSAGE (:type received)))
        (is (= "/queue/a" (get-in received [:headers :destination])))
        (is (= "cmb" (:body received)))))))

(deftest outstream-eof
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (clomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (clomp/writer s {:destination "/queue/a"})]
        (print "cmb")
        (flush)
        (.close *out*))
      (is (= "cmb" (:body (clomp/receive s))))
      (is (get-in (clomp/receive s) [:headers :eof])))))

(deftest instream
  (with-open [s  (java.net.Socket. "localhost" 61613)
              in (clomp/instream s)]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (clomp/subscribe s {:destination "/queue/a"})
      (clomp/send s {:destination "/queue/a"} "hrm\n")
      (clomp/send s {:destination "/queue/a"} "jlb\n")
      (clomp/send s {:destination "/queue/a"} "cmb\n")
      (binding [*in* (reader in)]
        (is (= "hrm" (read-line)))
        (is (= "jlb" (read-line)))
        (is (= "cmb" (read-line)))))))

(deftest instream-eof
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (clomp/subscribe s {:destination "/queue/a"})
      (clomp/send s {:destination "/queue/a"} "count to thirteen")
      (clomp/send s {:destination "/queue/a" :eof true} "")
      (binding [*in* (clomp/reader s)]
        (is (= "count to thirteen" (read-line)))
        (is (= nil (read-line)))))))

(deftest instream-outstream
  (with-open [s (java.net.Socket. "localhost" 61613)]
    (clomp/with-connection s {:login "foo" :password "secret"}
      (clomp/subscribe s {:destination "/queue/a"})
      (binding [*out* (clomp/writer s {:destination "/queue/a"})]
        (println "foo")
        (println "bar")
        (println "baz")
        (.close *out*))
      (binding [*in* (clomp/reader s)]
        (is (= "foo" (read-line)))
        (is (= "bar" (read-line)))
        (is (= "baz" (read-line)))))))
