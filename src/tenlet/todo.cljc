(ns tenlet.todo)

'[::server]
'([x] design system for declaring handlers for server & clients
  ([x] repl workflow)
  ([x] :line accumulation and handler))
'([x] server should track client connections
  ([x] closing the server should close all connections)
  ([x] client socket closure should trigger handler))

'[::client]



'[::protocols]
'([x] NAWS)

'[::bugs]
'([x] some IAC chars are making it to input)
'([x] need error handling for writing/closing clients)

'[triggering :ignore when redundant char or line mode request
    could have :ignore check for specific chars?]
[\ÿ 255]
[\ý 253]
[\ 3]