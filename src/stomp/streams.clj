(ns stomp.streams
  (:require stomp))

(gen-class
 :name         stomp.OutputStream
 :prefix       output-
 :extends      java.io.OutputStream
 :state        state
 :init         init
 :constructors {[Object clojure.lang.IPersistentMap] []})

(defn output-init [mq headers]
  [[] [mq headers (atom [])]])

(defn output-write
  ([this b]
     (let [[_ _ buffer] (.state this)]
       (if (integer? b)
         (swap! buffer conj b)
         (swap! buffer into b))))
  ([this b off len]
     (let [[_ _ buffer] (.state this)]
       (swap! buffer into (take len (drop off b))))))

(defn output-flush [this]
  (let [[mq headers buffer] (.state this)]
    (swap! buffer
      (fn [buffer]
        (if (seq buffer)
          (stomp/send mq headers (apply str (map char buffer))))
        []))))

(defn output-close [this]
  (.flush this)
  (let [[mq headers _] (.state this)]
    (stomp/send mq (assoc headers :eof true) "")))

(gen-class
 :name            stomp.InputStream
 :prefix          input-
 :extends         java.io.InputStream
 :state           state
 :init            init
 :exposes-methods {read readSuper}
 :constructors    {[Object] []})

(defn input-init [mq]
  [[] [mq (atom nil)]])

(defn input-available [this]
  (let [{:keys [offset body]} @(second (.state this))]
    (if offset
      (- (count body) offset)
      0)))

(defn- read-frame [frame mq]
  (loop [{:keys [body headers] :as frame} (or frame (stomp/receive mq))
         offset (or (:offset frame) 0)]
    (if (:eof headers)
      (assoc frame :byte -1)
      (if (< offset (count body))
        (assoc frame :offset (inc offset) :byte (int (.charAt body offset)))
        (recur (stomp/receive mq) 0)))))

(defn input-read
  ([this b] (.readSuper this b))
  ([this b off len]
     ;; the default read implementation will read the entire stream until EOF.
     (let [len (min len (max 1 (.available this)))]
       (.readSuper this b off len)))
  ([this]
     (let [[mq frame] (.state this)]
       (:byte (swap! frame read-frame mq)))))