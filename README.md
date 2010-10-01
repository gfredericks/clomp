clj-stomp is a [Streaming Text Oriented Messaging Protocol](http://stomp.codehaus.org/Protocol) client for Clojure.

# Getting started

Install a [stomp broker](http://stomp.codehaus.org/Brokers) and start it up. Both
[StompServer](http://stompserver.rubyforge.org) and [CoilMQ](http://code.google.com/p/coilmq)
don't seem to handle `ack:auto` correctly, so I've been testing with [HornetQ](http://jboss.org/hornetq).

# Usage

You can connect to a message broker using a `java.net.Socket`:

    (require 'stomp)

    (with-open [s1 (java.net.Socket. "localhost" 61613)
                s2 (java.net.Socket. "localhost" 61613)]

      (stomp/with-connection s1 {:login "foo" :password "password"}
        (stomp/send s1 {:destination "/queue/foo"} "blah"))

      (stomp/with-connection s2 {:login "bar" :password "secret"}
        (stomp/subscribe s2 {:destination "/queue/foo"})
        (:body (stomp/receive s2))))

    => "blah"

# Streams

clj-stomp also supports attaching streams to message queues:

    (with-open [s1  (java.net.Socket. "localhost" 61613)
                s2  (java.net.Socket. "localhost" 61613)
                out (stomp/writer s1 {:destination "/queue/a"})
                in  (stomp/reader s2)]

      (stomp/with-connection s1 {:login "foo" :password "password"}
        (binding [*out* out]
          (println "foo")
          (println "bar")
          (println "baz")))

      (stomp/with-connection s2 {:login "bar" :password "secret"}
        (stomp/subscribe s2 {:destination "/queue/a"})
        (binding [*in* in]
          [(read-line)
           (read-line)
           (read-line)))))

    => ["foo", "bar", "baz"]
