clj-stomp is a [Streaming Text Oriented Messaging Protocol](http://stomp.codehaus.org/Protocol) client for Clojure.

# Getting started

Install a [stomp broker](http://stomp.codehaus.org/Brokers) and start it up.
A quick and dirty one is stompserver:

    gem install stompserver
    stompserver

Now you can connect using a `java.net.Socket`

    (require 'stomp)
    (def s (java.net.Socket. "localhost" 61613))

    (stomp/with-connection s {}
      (stomp/subscribe s {:destination "/queue/foo"})
      (stomp/send s {:destination "/queue/foo"} "blah")
      (stomp/receive s))
    ; => #:stomp.Frame{:type :MESSAGE, :headers {:content-length "4", :destination "/queue/foo"}, :body "blah"}