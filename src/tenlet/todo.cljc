(ns tenlet.todo)

'[::server]
'([/] design system for declaring handlers for server & clients
  ([x] repl workflow)
  ([ ] :line accumulation and handler))
'([ ] server should track client connections)
'([ ] closing the server should close all connections)



'[::protocols]
'([x] NAWS
  ([ ] IAC routing))

'[::bugs]
'([ ] some IAC chars are making it to input)