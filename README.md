clj-stomp is a [stomp](http://stomp.codehaus.org/Protocol) client for clojure

# Getting started

Install a [stomp broker](http://stomp.codehaus.org/Brokers) and start it up.
A quick and dirty one is stompserver:

    gem install stompserver
    stompserver

Now you can connect using a `java.net.Socket`

    (require 'stomp)
    (def s (java.net.Socket. "localhost" 61613))

    (stomp/connect s {})
    ; => #:stomp.Frame{:type :CONNECTED, :headers {:session "wow"}, :body ""}

    (stomp/subscribe s {:destination "/queue/foo"})
    (stomp/send s {:destination "/queue/foo"} "blah")
    (stomp/receive s)
    ; => #:stomp.Frame{:type :MESSAGE, :headers {:content-length "4", :destination "/queue/foo"}, :body "blah"}    
    
    (stomp/disconnect)