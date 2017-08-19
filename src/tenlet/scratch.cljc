(ns tenlet.scratch
  (:require 
    [tenlet.server :refer [write close create-server]]
    [tenlet.escape :as esc]))

(def players (atom {}))

(defn broad! [& args]
  (let [s (apply str args)]
    (dorun (map #(write % s) (keys @players)))))

(defn new-player [c]
  (swap! players assoc c {})
  (prn [:new c])
  (prn (type (.-socket c)))
  (write c (str esc/IAC esc/DO esc/NAWS))
  (write c (str esc/IAC esc/DO esc/LINE))
  )


(defn player-input [c s] (prn s))

(defn player-quit [c]
  (swap! players dissoc c)
  (broad! :dissconect c))

(defn player-resize [c m]
  (let [{:keys [w h]} m]
    (write c esc/CLR)
    (dorun 
      (for [x (range (inc w))
            y (range (inc h))
            :when (or (#{2 (dec w)} x) (#{2 (dec h)} y))]
      (do 
        (write c (esc/cursor x y))
        (write c (str 
          (esc/background (rand-nth (vec esc/color-names))) 
          (esc/code (rand-nth (vec esc/color-names)))))
        (write c (char (+ 33 (rand-int 93)))))))
    (write c (esc/cursor (int (/ w 2)) (int (/ h 2))))
    (write c (str (esc/code :red) m (esc/code :reset)))))

(def server 
  (create-server 5071 {
    :connect  #'new-player
    :line     #'player-input
    :input    #'player-input
    :close    #'player-quit
    :shutdown #(prn :shutdown! %)
    :resize   #'player-resize}))

(defn shutdown! []
  (broad! 
    (esc/background :white) (esc/code :red)
    "\nSERVER SHUTTING DOWN\n"
    (esc/code :reset))
  (dorun (map #(close %) (keys @players)))
  (.close server))

'(shutdown!)

(for [i (range 20)] 
  (broad! 
    (esc/background (rand-nth (vec esc/color-names))) 
    (esc/code (rand-nth (vec esc/color-names)))
    'selfsame (esc/code :reset)))

'(for [i (range 20)]
  (do 
    (broad! (cursor i i))
    (broad! (code (rand-nth (vec color-names))))
    (broad! "@")))




;echo
'(broad! IAC WONT ECHO)
;no echo
'(broad! IAC WILL ECHO)

;enter char mode
'(broad! IAC DO LINE)


'(broad! IAC DONT LINE)

;not sure
'(broad! ansi-esc ORIG )

;request screen size reports
'(broad! IAC DO NAWS)

;clear screen
'(broad! CLR)

;hide cursor
'(broad! CSI HIDE)