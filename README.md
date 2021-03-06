# tenlet - a clojure telnet library

`[selfsame/tenlet "0.2"]`


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

`tenlet.server` has two user protocols:
* `(write [client-or-server string])` 
* `(close [client-or-server])`

Writing or closing a server writes or closes all open clients.


The `:line` handler recieves typical newline delimited input from the client.  `:input` recieves single character input, and is meant to be used with `IAC DO LINE` mode where individual characters are sent over the socket. `:input` also recieves keywords from mutli character codes (`:arrow-left`, `:pagedown`, etc.)


`tenlet.escape` namespace has protocol defs and escape code fns for styling, placing the cursor, etc. To enable `:resize` events, tell the client to use NAWS reporting `(write client (str IAC DO NAWS))`.

### wishlist

- [ ] cross platform
  - [x] `:clj`
  - [x] `:cljs`
  - [ ] `:cljc`
- [x] user handlers for server & client lifecycles
- [x] Telnet protocol code defs
- [x] NAWS terminal size reporting
- [ ] helpers for line/char, echo, cursor modes
- [x] string formatting & escape code decorating namespace