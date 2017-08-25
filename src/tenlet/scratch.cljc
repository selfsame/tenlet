(ns tenlet.scratch
  (:require 
    [tenlet.server :refer [write close create-server DEBUG]]
    [tenlet.escape :as esc]))

(def players (atom {}))

(defonce world (atom {
  [5 5] {:color :red :char "$"}}))

(defn broad! [& args]
  (let [s (apply str args)]
    (dorun (map #(write % s) (keys @players)))))

(defn state 
  ([c] (get @players c))
  ([c k] (get-in @players [c k])))

(defn set-state! [c k v]
  (swap! players update c assoc k v))

(defn update-state! [c k f]
  (swap! players update c update k f))

(defn cursor! [c x y]
  (write c (esc/cursor x y)))


(defn draw-world [c]
  (let [{:keys [w h]} (state c :window)
        [x y] (state c :pos)
        [ox oy] (mapv int (mapv * [w h] [0.5 0.5]))]
    (write c esc/CLR)
    (cursor! c ox oy)
    (write c "@")
    (dorun 
      (for [xx (range w)
            yy (range h)
            :let [tile (get @world [xx yy])]
            :when tile]
      (do 
        (cursor! c xx yy)
        (write c (:char tile)))

      ))))

(defn new-player [c]
  (swap! players assoc c {
    :pos [0 0]
    :window {:w 10 :h 10}})
  (write c esc/naws)
  (write c esc/char-mode)
  (write c esc/no-echo)
  (write c esc/CLR)
  (draw-world c))


(def move-map {
  :arrow-left   [-1 0]
  :arrow-right  [1 0]
  :arrow-up     [0 1]
  :arrow-down   [0 -1]})

(defn player-input [c s] (prn s)
  (if-let [delta (move-map s)]

    ))

(defn player-quit [c]
  (swap! players dissoc c)
  (prn :disconnect c)
  (broad! "\n" :disconnect " " c "\n"))

(defn player-resize [c m]
  (let [{:keys [w h]} m]
    (set-state! c :window m)
    (draw-world c)))

(declare server)

(defn shutdown! []
  (write server (str  
    (esc/background :white) (esc/code :red)
    "\nSERVER SHUTTING DOWN\n"
    (esc/code :reset)))
  (close server))

(if server (shutdown!))

(def server 
  (create-server 5071 {
    :connect  #'new-player
    :line     #'player-input
    :input    #'player-input
    :close    #'player-quit
    :shutdown #(prn :shutdown! %)
    :resize   #'player-resize}))






'(swap! DEBUG not)

;echo
'(broad! esc/IAC esc/WONT esc/ECHO)
;no echo
'(broad! esc/IAC esc/WILL esc/ECHO)

;enter char mode
'(broad! esc/char-mode)

;enter line mode
'(broad! esc/line-mode)


'(broad! esc/IAC esc/DONT esc/LINE)

;not sure
'(broad! ansi-esc ORIG )

;request screen size reports
'(broad! IAC DO NAWS)

;clear screen
'(broad! CLR)

;hide cursor
'(broad! CSI HIDE)