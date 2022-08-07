(ns tenlet.server
  (:require
    [tenlet.decode :as decode])
  #?(:clj (:import
            [java.net ServerSocket Socket SocketException]
            [java.io InputStreamReader OutputStreamWriter BufferedWriter]
            [java.nio.charset Charset])))

(def DEBUG (atom false))

(defn debug [& args]
  (if @DEBUG (apply prn args)))


(def iso-latin-1 #?(
  :clj (Charset/forName "ISO-8859-1")
  :cljs nil))

#?(:cljs (def net (js/require "net")))

#?(:clj (defn- on-thread [f] (doto (new Thread f) (.start)))
   :cljs nil)

(defn- call [opts k & args]
  (if-let [f (get opts k)]
    (apply f args)))

(defprotocol ITelnet
  (connect [o] [o c])
  (write [o s])
  (input [o c])
  (line [o s])
  (close [o]))

(deftype Server [socket opts]
  ITelnet
  (connect [o c]
    (swap! opts update :clients conj c))
  (write [o s]
    (dorun (map #(write % s) (:clients @opts))))
  (close [o]
    (dorun (map close (:clients @opts)))
    (.close socket)))

(deftype Client [socket ow opts]
  ITelnet
  (connect [o] (call @opts :connect o))
#?(:clj
  (write [o s]
    (try
      (.write ow (str s))
      (.flush ow)
      (catch Exception e)))
  :cljs
  (write [o s]
    (try
      (.write socket (str s) "latin1")
      (catch :default e))))
  (input [o c]
    (debug [c (int c)])
    (let [code (decode/op (or (::code @opts) {}) c)
          out (:out code)
          -line (:line code)
          resize (:resize code)]
      (debug :code code)
      (when resize
        (call @opts :resize o resize)
        (swap! opts assoc ::code (dissoc code :resize)))
      (when out
        (call @opts :input o out)
        (swap! opts assoc ::code (dissoc code :out)))
      (when -line
        (line o -line)
        (swap! opts assoc ::code {}))
      (if-not (or out -line resize)
        (swap! opts assoc ::code code))))
  (line [o s] (call @opts :line o s))
  (close [o]
    (swap! (.-opts (::server @opts)) update :clients
      #(-> % set (disj o) seq))
    #?(:clj
        (try
          (.close socket)
          (call @opts :close o)
          (catch Exception e))
       :cljs
        (try
          (.destroy socket)
          (catch :default e)))))

(defn- new-connection [S c opts]
 #?(:clj
    (on-thread
      (fn []
        (let [i (. c (getInputStream))
              o (. c (getOutputStream))
              ir (new InputStreamReader i iso-latin-1)
              ow (new OutputStreamWriter o iso-latin-1)
              C (Client. c ow (atom opts))]
          (connect S C)
          (connect C)
          ((fn []
            (when-not (.isClosed c)
              (try
                (input C (char (.read ir)))
                (catch java.lang.IllegalArgumentException e
                  (close C))
                (catch java.net.SocketException e))
              (recur)))))))
    :cljs
    (let [C (Client. c nil (atom opts))]
      (.setEncoding c "latin1")
      (connect S C)
      (connect C)
      (.on c "data" (fn [d] (dorun (map #(input C %) (str d)))))
      (.on c "close"
        (fn [e]
          (close C)
          (call @(.-opts C) :close C))))))

(defn create-server [port opts]
 #?(:clj
    (let [s (new ServerSocket port)
          S (Server. s (atom {}))
          opts (assoc opts ::server S)]
      (on-thread
        (fn []
          (when-not (.isClosed s)
            (try
              (let [c (.accept s)]
                (new-connection S c opts))
              (catch SocketException e
                (call opts :shutdown S)))
            (recur)))) S)
    :cljs
    (let [s ((.-createServer net))
          S (Server. s (atom {}))
          opts (assoc opts ::server S)]
      (.listen s port)
      (.on s "connection" (fn [c]
        (new-connection S c opts)))
      (.on s "close"
        (fn [e] (call opts :shutdown S))) S)))
