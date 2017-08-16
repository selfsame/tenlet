# tenlet - a clojure telnet library


```clj
(use 'tenlet.server)

(def server 
  (create-server 5073 {
    :connect  (fn [client])
    :line     (fn [client string])
    :input    (fn [client char-or-keyword])
    :resize   (fn [client {:keys [w h]}])
    :close    (fn [client])
    :shutdown (fn [server])}))
```

`tenlet.server.Client` has two user protocols:
* `(write [client string])` 
* `(close [client])`


The `:line` handler recieves typical newline delimited input from the client.  `:input` recieves single character input, and is meant to be used with `IAC DO LINE` mode where individual characters are sent over the socket. `:input` also recieves keywords from mutli character codes (`:arrow-left`, `:pagedown`, etc.)


`tenlet.escape` namespace has protocol defs and escape code fns for styling, placing the cursor, etc. To enable `:resize` events, tell the client to enter NAWS reporting `(write client (str IAC DO NAWS))`.

### wishlist

- [ ] cross platform (clj/cljr/cljs) socket server
- [x] user handlers for server & client lifecycles
- [ ] Telnet protocol code defs
- [x] NAWS terminal size reporting
- [ ] helpers for line/char, echo, cursor modes
- [x] string formatting & escape code decorating namespace