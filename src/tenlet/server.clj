(ns tenlet.server
  (:use 
    tenlet.escape)
  (:require
    [tenlet.decode :as decode])
  (import 
    [java.net ServerSocket Socket SocketException]
    [java.io InputStreamReader OutputStreamWriter BufferedWriter]
    [java.nio.charset Charset]))

(def DEBUG (atom false))

(defn debug [& args]
  (if @DEBUG (apply prn args)))


(def iso-latin-1  (Charset/forName "ISO-8859-1"))

(defn- on-thread [f] (doto (new Thread f) (.start)))

(defn- call [opts k & args]
  (if-let [f (get opts k)]
    (apply f args)))

(defprotocol IClient
  (connect [o])
  (write [o s])
  (input [o c])
  (line [o s])
  (close [o]))

(deftype Client [socket ir ow opts]
  IClient
  (connect [o] (call @opts :connect o))
  (write [o s]
    (.write ow (str s)) 
    (.flush ow))
  (input [o c] 
    (debug [c (int c)])
    (let [code (decode/op (or (::code @opts) {}) c)
          out (:out code)
          -line (:line code)
          resize (:resize code)]
      (debug :code code)
      (when resize 
        (call @opts :resize o resize))
      (when out
        (call @opts :input o out)
        (swap! opts assoc ::code (dissoc code :out)))
      (when -line
        (line o -line)
        (swap! opts assoc ::code {}))
      (if-not (or out -line)
        (swap! opts assoc ::code code))))
  (line [o s] (call @opts :line o s))
  (close [o] 
    (call @opts :close o)
    (.close socket)))

(defn- new-connection [c opts] 
  (on-thread 
    (fn [] 
      (let [i (. c (getInputStream)) 
            o (. c (getOutputStream))
            ir (new InputStreamReader i iso-latin-1)
            ow (new OutputStreamWriter o iso-latin-1)
            client (Client. c ir ow (atom opts))]
        (connect client)
        ((fn [] 
          (when-not (.isClosed c)
            (try 
              (input client (char (.read ir)))
              (catch java.lang.IllegalArgumentException e
                (close client))
              (catch java.net.SocketException e))
            (recur))))))))

(defn create-server [port opts]
  (let [s (new ServerSocket port)]
    (on-thread 
      (fn [] 
        (when-not (.isClosed s)
          (try (new-connection (.accept s) opts)
               (catch SocketException e
                 (call opts :shutdown s)))
          (recur)))) s))