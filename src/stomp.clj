(ns stomp
  (:refer-clojure :exclude [send])
  (:use [clojure.string :only [join]]
        [clojure.java.io :only [reader writer]])
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

(defmacro with-connection [mq headers & forms]
  `(let [frame# (connect ~mq ~headers)]
     (if-not (= :CONNECTED (:type frame#))
       (throw (Exception. (:message frame#))))
     (binding [*connection* ~mq
               *session-id* (get-in frame# [:headers :session-id])]
       (try
         ~@forms
         (finally
          (disconnect ~mq))))))

(defn- send-frame [socket command headers & [body]]
  (binding [*out* (writer socket)]
    (println command)
    (println (join "\n" (map (comp (partial join ":") (partial map name)) headers)))
    (if body      
      (println (str "content-length:" (count body))))
    (println)
    (if body
      (print body))
    (print "\0")
    (flush)))

(defn- read-headers []
  (loop [headers {}]
    (let [[key val] (.split (read-line) ":")]
      (if val
        (recur (assoc headers (keyword key) val))
        headers))))

(defn- read-body [length]
  (if length
    (let [length (Integer/parseInt length)
          buffer (char-array length)]
      (.read *in* buffer 0 length)
      (.read *in*)
      (String. buffer))
    (loop [string ""]
      (let [c (.read *in*)]
        (if (= 0 c)
          string
          (recur (str string (char c))))))))

(defrecord Frame [type headers body])

(defn- receive-frame [socket]
  (binding [*in* (reader socket)]
    (let [type    (keyword (read-line))
          headers (read-headers)
          body    (read-body (:content-length headers))]
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
