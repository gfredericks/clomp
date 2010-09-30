(ns stomp
  (:refer-clojure :exclude [send])
  (:import [java.io BufferedInputStream InputStreamReader BufferedReader BufferedOutputStream OutputStreamWriter BufferedWriter]
           [java.net Socket]))

(defn- make-header [headers]
  (->> (for [[key value] headers]
         (str (name key) ":" value))
       (interpose "\n")
       (apply str)))

(defn- send-frame [socket command headers & [body]]
  (binding [*out* (BufferedWriter. (OutputStreamWriter. (BufferedOutputStream. (.getOutputStream socket))))]
    (println command)
    (println (make-header headers))
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

(defstruct Frame :type :headers :body)

(defn- receive-frame [socket]
  (binding [*in* (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream socket))))]
    (let [type    (keyword (read-line))
          headers (read-headers)
          body    (read-body (:content-length headers))]
      (struct Frame type headers body))))

(defn connect     [s headers]      (send-frame s "CONNECT"     headers) (receive-frame s))
(defn send        [s headers body] (send-frame s "SEND"        headers body))
(defn subscribe   [s headers]      (send-frame s "SUBSCRIBE"   headers))
(defn unsubscribe [s headers]      (send-frame s "UNSUBSCRIBE" headers))
(defn begin       [s headers]      (send-frame s "BEGIN"       headers))
(defn commit      [s headers]      (send-frame s "COMMIT"      headers))
(defn abort       [s headers]      (send-frame s "ABORT"       headers))
(defn ack         [s headers]      (send-frame s "ACK"         headers))
(defn disconnect  [s]              (send-frame s "DISCONNECT"  {}))
(defn receive     [s]              (receive-frame s))