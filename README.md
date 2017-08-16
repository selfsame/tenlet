# tenlet - a clojure telnet library

```clj
(use 'tenlet.server)

(def server 
  (create-server 5073 {
    :connect  (fn [client])
    :input    (fn [client chr])
    :line     (fn [client string])
    :resize   (fn [client {:keys [w h]}])
    :close    (fn [client])
    :shutdown (fn [server])}))
```

`tenlet.escape` namespace has escape code defs and fns for styling, placing the cursor, etc.

### wishlist

* cross platform (clj/cljr/cljs) socket server
* user handlers for server & client lifecycles
* Telnet Protocol code defs
* helpers for line/char mode input and NAWS terminal size reporting
* string formatting & escape code decorating namespace