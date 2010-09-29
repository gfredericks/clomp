(ns stomp
  (:refer-clojure :exclude [send])
  (:use [clojure.string :only [join]]
        [clojure.java.io :only [reader writer]])
  (:import [java.net Socket]))

(defprotocol Stomp  
  (connect     [s headers])
  (subscribe   [s headers])
  (unsubscribe [s headers])
  (begin       [s headers])
  (commit      [s headers])
  (abort       [s headers])
  (ack         [s headers])
  (disconnect  [s])
  (send        [s headers body])
  (receive     [s]))

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

(def default-stomp-impl
  {:connect     (fn [s headers]      (send-frame s "CONNECT"     headers) (receive-frame s))
   :send        (fn [s headers body] (send-frame s "SEND"        headers body))
   :subscribe   (fn [s headers]      (send-frame s "SUBSCRIBE"   headers))
   :unsubscribe (fn [s headers]      (send-frame s "UNSUBSCRIBE" headers))
   :begin       (fn [s headers]      (send-frame s "BEGIN"       headers))
   :commit      (fn [s headers]      (send-frame s "COMMIT"      headers))
   :abort       (fn [s headers]      (send-frame s "ABORT"       headers))
   :ack         (fn [s headers]      (send-frame s "ACK"         headers))
   :disconnect  (fn [s]              (send-frame s "DISCONNECT"  {}))
   :receive     (fn [s]              (receive-frame s))})

(extend Socket
  Stomp default-stomp-impl)