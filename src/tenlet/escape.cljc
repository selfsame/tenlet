(ns tenlet.escape
  (:require 
    clojure.string))

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

(def ansi-esc (str (char 27) \[ ))

(defn escape [n] 
  (let [n (if (sequential? n) (clojure.string/join ";" n) n)] 
      (str ansi-esc n "m")))

(defn cursor [x y] (str ansi-esc y ";" x "H"))

(def color-names #{:black :red :green :yellow :blue :magenta :cyan :white})

(def code-map {
  :resetall      0
  :bold          1
  :underline     4
  :blink         5
  :reverse       7
  :boldoff      22
  :blinkoff     25
  :underlineoff 24
  :reverseoff   27
  :reset         0
  :black        30
  :red          31
  :green        32
  :yellow       33
  :blue         34
  :magenta      35
  :cyan         36
  :white        37})

(defn code [k]
  (if-let [n (code-map k)]
    (escape n)))

(defn background [k]
  (if-let [n (code-map k)]
    (escape (+ n 10))))

(defn blink [& args]
  (str (escape 5) (apply str args) (escape 25)))

(def echo       (str IAC WONT ECHO))
(def no-echo    (str IAC WILL ECHO))

(defn- -line-mode [mode]
  (str IAC DO LINE IAC SB LINE (char 1) (char mode) IAC SE))

(def char-mode (-line-mode 0))
(def line-mode (-line-mode 1))
(def naws (str IAC DO NAWS))

(def hide-cursor (str CSI HIDE))