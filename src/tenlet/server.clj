(ns tenlet.server
  (import 
    [java.net ServerSocket Socket SocketException]
    [java.io InputStreamReader OutputStreamWriter BufferedWriter]
    [java.nio.charset Charset]))

(def iso-latin-1  (Charset/forName "ISO-8859-1"))

(def  IAC  "\u00ff") ;255
(def  WILL "\u00fb") ;251
(def  WONT "\u00fc") ;252
(def  DO   "\u00fd") ;253
(def  DONT "\u00fe") ;254
(def  SE   "\u00f0") ;240
(def  SB   "\u00fa") ;250

(def  CRLF "\r\n")

(def  ECHO "\u0001") ;  1
(def  LINE "\u0022") ; 34
(def  NAWS "\u001f") ; 31

(def  CSI  "\u001b[")
(def  CLR  "\033[2J")
(def  ORIG "1;1H")
(def  HIDE "?25l")   ; tput civis [man terminfo]
(def  SHOW "?25h")   ; tput cnorm [man terminfo]

(def ansi-esc (String. (byte-array [27 (int \[)])))

(defn cursor [x y] (str ansi-esc x ";" y "H"))


(defn- on-thread [f] (doto (new Thread f) (.start)))

(defn- call [opts k & args]
  (if-let [f (get opts k)]
    (apply f args)))

(defprotocol IClient
  (connect [o])
  (write [o s])
  (input [o c])
  (close [o]))

(deftype Client [socket ir ow opts]
  IClient
  (connect [o] (call @opts :connect o))
  (write [o s]
    (.write ow (str s)) 
    (.flush ow))
  (input [o c] (call @opts :input o c))
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