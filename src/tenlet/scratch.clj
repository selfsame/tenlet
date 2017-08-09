(ns tenlet.scratch
  (:use tenlet.server))

(def players (atom {}))

(defn broad! [& args]
  (let [s (apply str args)]
    (dorun (map #(write % s) (keys @players)))))

(defn new-player [c]
  (swap! players assoc c {})
  (write c :welcome!))

(defn player-input [c i]
  (prn i))

(defn player-quit [c]
  (swap! players dissoc c)
  (broad! :dissconect c))

(def server 
  (create-server 5073 {
    :connect #'new-player
    :input   #'player-input
    :close   #'player-quit
    :shutdown #(prn :shutdown! %)}))

(defn shutdown! []
  (broad! "\nSERVER SHUTTING DOWN\n")
  (dorun (map #(close %) (keys @players)))
  (.close server))

'(shutdown!)

;echo
(broad! IAC WONT ECHO)
;no echo
(broad! IAC WILL ECHO)

;enter char mode
(broad! IAC DO LINE)


(broad! IAC DONT LINE)

;not sure
(broad! ansi-esc ORIG )

;request screen size reports
(broad! IAC DO NAWS)

;clear screen
(broad! CLR)

;hide cursor
(broad! CSI HIDE)


(for [i (range 20)]
  (do 
    (broad! (cursor i i))
    (broad! "@")))