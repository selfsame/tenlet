(ns server.core
    (:require 
      [com.gearswithingears.async-sockets :refer :all]
      [clojure.core.async :as async]))

(defonce server (socket-server 5072))
(def running (atom true))

(defn handle [socket in]
  (prn [in socket])
  (async/go (async/>! (:out socket) (str "ECHO2: " in))))

(defn echo-everything [socket]
  (async/go-loop []
    (when-let [line (async/<! (:in socket))]
      (handle socket line)
      (recur))))
   
'(async/go-loop []
  (if @running 
    (when-let [connection (async/<! (:connections server))] 
      (echo-everything connection)
      (recur))))

'(reset! running true)