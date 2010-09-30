clj-stomp is a [Streaming Text Oriented Messaging Protocol](http://stomp.codehaus.org/Protocol) client for Clojure.

# Getting started

Install a [stomp broker](http://stomp.codehaus.org/Brokers) and start it up.
A quick and dirty one is stompserver:

    gem install stompserver
    stompserver

Now you can connect using a `java.net.Socket`

    (require 'stomp)
    (def s1 (java.net.Socket. "localhost" 61613))
    (def s2 (java.net.Socket. "localhost" 61613))

    (stomp/with-connection s1 {:login "foo" :password "password"}
      (stomp/send s1 {:destination "/queue/foo"} "blah"))

    (stomp/with-connection s2 {:login "bar" :password "secret"}
      (stomp/subscribe s2 {:destination "/queue/foo"})
      (stomp/receive s2))
    ; => #:stomp.Frame{:type :MESSAGE, :headers {:content-length "4", :destination "/queue/foo"}, :body "blah"}