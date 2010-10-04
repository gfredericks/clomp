(ns stomp
  (:refer-clojure :exclude [send])
  (:use [clojure.string :only [join]])
  (:require [clojure.java.io :as io])
  (:import [java.net Socket]))

(defprotocol Stomp
  (connect     [mq headers])
  (send        [mq headers body])
  (subscribe   [mq headers])
  (unsubscribe [mq headers])
  (begin       [mq headers])
  (commit      [mq headers])
  (abort       [mq headers])
  (ack         [mq headers])
  (disconnect  [mq])
  (receive     [mq])
  (clone       [mq]))

(def *session-id* nil)
(def *connection* nil)

(defn assert-frame-type [type frame]
  (if-not (= type (:type frame))
    (throw (Exception. (str (get-in frame [:headers :message]) "\n" (:body frame))))))

(defmacro with-connection [mq headers & forms]
  `(let [frame# (connect ~mq ~headers)]
     (assert-frame-type :CONNECTED frame#)
     (binding [*connection* ~mq
               *session-id* (get-in frame# [:headers :session])]
       (try
         ~@forms
         (finally
          (disconnect ~mq))))))

(defn- send-frame [out command headers & [body]]
  (binding [*out* (io/writer out)]
    (println command)
    (println (join "\n" (for [[k v] headers] (str (name k) ":" v))))
    (if body (println (str "content-length:" (count body))))
    (println)
    (if body (print body))
    (print "\0")
    (flush)))

(defn- read-headers []
  (loop [headers {}]
    (let [[key val] (.split (read-line) ":")]
      (if val
        (recur (assoc headers (keyword key) val))
        headers))))

(defn- read-body [in length]
  (if length
    (let [length (Integer/parseInt length)
          buffer (char-array length)]
      (loop [offset 0]
        (if (< offset length)
          (recur (+ (.read in buffer offset (- length offset))
                    offset ))))
      (.read in) ;; consume \0
      (String. buffer))
    (loop [string ""]
      (let [c (.read in)]
        (if (= 0 c)
          string
          (recur (str string (char c))))))))

(defrecord Frame [type headers body])

(defn- receive-frame [in]
  (binding [*in* (io/reader in)]
    (let [type    (keyword (read-line))
          headers (read-headers)
          body    (read-body *in* (:content-length headers))]
      (Frame. type headers body))))

(extend-type Socket
  Stomp
  (connect     [s headers]      (send-frame s "CONNECT"     headers) (receive-frame s))
  (send        [s headers body] (send-frame s "SEND"        headers body))
  (subscribe   [s headers]      (send-frame s "SUBSCRIBE"   headers))
  (unsubscribe [s headers]      (send-frame s "UNSUBSCRIBE" headers))
  (begin       [s headers]      (send-frame s "BEGIN"       headers))
  (commit      [s headers]      (send-frame s "COMMIT"      headers))
  (abort       [s headers]      (send-frame s "ABORT"       headers))
  (ack         [s headers]      (send-frame s "ACK"         headers))
  (disconnect  [s]              (send-frame s "DISCONNECT"  {}))
  (receive     [s]              (receive-frame s))
  (clone       [s] (doto (Socket.) (.connect (.getRemoteSocketAddress s)))))

(defmacro outstream [mq headers]
  `(stomp.OutputStream. ~mq ~headers))

(defmacro instream [mq]
  `(stomp.InputStream. ~mq))

(defmacro writer [mq headers]
  `(io/writer (outstream ~mq ~headers)))

(defmacro reader [mq]
  `(io/reader (instream ~mq)))
