(ns tenlet.server
  (import 
    [java.net ServerSocket Socket SocketException]
    [java.io InputStreamReader OutputStreamWriter BufferedWriter]
    [clojure.lang LineNumberingPushbackReader]
    [java.nio.charset Charset]))

(def iso-latin-1  (Charset/forName "ISO-8859-1"))
(def utf-8        (Charset/forName "UTF-8"))
(def ascii        (Charset/forName "US-ASCII"))

(def  T_IAC  "\u00ff");255
(def  T_WILL "\u00fb");251
(def  T_WONT "\u00fc");252
(def  T_DO   "\u00fd");253
(def  T_DONT "\u00fe");254
(def  T_SE   "\u00f0");240
(def  T_SB   "\u00fa");250

(def  T_CRLF "\r\n")

(def  T_ECHO "\u0001");  1
(def  T_LINE "\u0022"); 34
(def  T_NAWS "\u001f"); 31

(def  T_CSI  "\u001b[")
(def  T_CLR  "\033[2J")
(def  T_ORIG "1;1H")
(def  T_HIDE "?25l") ; tput civis [man terminfo]
(def  T_SHOW "?25h") ; tput cnorm [man terminfo]

(def ansi-esc (String. (byte-array [27 (int \[)])))

(defn cursor [x y] (str ansi-esc x ";" y "H"))


(def OUT (atom nil))

(defn on-thread [f]
  (doto (new Thread f) (.start)))

(defn create-server [accept-socket port]
  (let [ss (new ServerSocket port)]
    (on-thread #(when-not (. ss (isClosed))
                  (try (accept-socket (. ss (accept)))
                       (catch SocketException e
                        (prn e)))
                  (recur))) ss))


(defn new-connection [s] 
  (on-thread 
    (fn [] 
      (let [i (. s (getInputStream)) 
            o (. s (getOutputStream))
            ir (new InputStreamReader i iso-latin-1)
            ow (new OutputStreamWriter o iso-latin-1)]
        ::client-connect
        (reset! OUT ow)
        (#(when-not (. s (isClosed))
          (try 
            (let [c (char (.read ir))]
              ::client-input
              (prn c))
            (catch java.lang.IllegalArgumentException e
              ::client-close
              (.close s)))
          (recur)))))))


(defn send! [& s]
  (.write @OUT (apply str s)) 
  (.flush @OUT))


(def server (create-server new-connection 5073))