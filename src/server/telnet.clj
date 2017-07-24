(ns server.telnet
  (import 
    [java.net ServerSocket Socket SocketException]
    [java.io InputStreamReader OutputStreamWriter BufferedWriter]
    [clojure.lang LineNumberingPushbackReader]
    [java.nio.charset Charset]))

(def iso-latin-1 "The ISO Latin-1 charset object" (Charset/forName "ISO-8859-1"))
(def utf-8 "The UTF-8 charset object" (Charset/forName "UTF-8"))
(def ascii "The ASCII charset object" (Charset/forName "US-ASCII"))

(def  T_IAC  "\u00ff" );255 "\377"
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

(send! (str ansi-esc 41 "m" "hello"))

(def OUT (atom nil))

(defn on-thread [f]
  (doto (new Thread f) (.start)))

(defn create-server [accept-socket port]
  (let [ss (new ServerSocket port)]
    (on-thread #(when-not (. ss (isClosed))
                  (try (accept-socket (. ss (accept)))
                       (catch SocketException e))
                  (recur))) ss))


(defn new-connection [s] 
  (on-thread 
    (fn [] 
      (let [i (. s (getInputStream)) 
            o (. s (getOutputStream))
            ir (new InputStreamReader i iso-latin-1)
            ow (new OutputStreamWriter o iso-latin-1)]
    (reset! OUT ow)
    (while (not (. s (isClosed)))
      (let [c (char (.read ir))]
        (prn c)))))))

(def server (create-server new-connection 5073))

'(. server (close))

(defn send! [& s]
  (.write @OUT (apply str s)) 
  (.flush @OUT))


(send! (String. (byte-array [255 253 34 255 250 34 1 0 255 240 255 251 1]), iso-latin-1))

(send! (String. (byte-array [255 252 1]), iso-latin-1))
(send! (String. (byte-array [255 251 1]), iso-latin-1))

(send! (str T_IAC T_WONT T_ECHO))
(send! (str T_IAC T_WILL T_ECHO))
(send! T_IAC T_DO T_LINE)
(send! T_IAC T_DONT T_LINE)
(send! ansi-esc T_ORIG )

(send! T_IAC T_DO T_NAWS)

(defn cursor [x y]
  (str ansi-esc x ";" y "H"))

(send! T_CLR)
(for [i (range 20)]
  (do 
    (send! (cursor i i))
    (send! "@")))

(send! ansi-esc "40;40H")

(str T_NAWS)

(int \ú)

(int \ð)

(str T_IAC)