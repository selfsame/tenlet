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
  (broad! "SERVER SHUTTING DOWN")
  (dorun (map #(close %) (keys @players)))
  (.close server))

'(shutdown!)

;echo
(broad! T_IAC T_WONT T_ECHO)
;no echo
(broad! T_IAC T_WILL T_ECHO)

;enter char mode
(broad! T_IAC T_DO T_LINE)


(broad! T_IAC T_DONT T_LINE)

;not sure
(broad! ansi-esc T_ORIG )

;request screen size reports
(broad! T_IAC T_DO T_NAWS)

;clear screen
(broad! T_CLR)

;hide cursor
(broad! T_CSI T_HIDE)


(for [i (range 20)]
  (do 
    (broad! (cursor i i))
    (broad! "@")))