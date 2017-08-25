(ns tenlet.scratch
  (:require 
    [tenlet.server :refer [write close create-server DEBUG]]
    [tenlet.escape :as esc :refer [code background color-names]]))

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

(defn write! [c & more]
  (write c (apply str more)))

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
            :let [[x2 y2] (mapv - (mapv + [xx yy] [x y]) [ox oy])
                  tile (get @world [x2 y2])]
            :when tile]
      (do 
        (cursor! c xx yy)
        (write! c 
          (:char tile))) ))
    (cursor! c 1 (- h 1))
    (write c (apply str (take w (repeat \-))))
    (cursor! c 1 h)
    (write c (apply str (take w (repeat " "))))
    (write! c 
      (esc/cursor 1 h)
      "color <" (code (state c :color)) "#" (code :reset)
      "> (pageup/down)    "
      "background <" (background (state c :background)) " " (code :reset)
      "> (home/end)"
      (esc/cursor (- w 10) h)
      (code :green) "(" x " " y ")   " (code :reset)) ))

(defn update-vision [x y]
  (dorun (map
    (fn [[c m]]
      (let [[px py] (:pos m)
            {:keys [w h]} (:window m)
            [ox oy] (mapv int (mapv * [w h] [0.5 0.5]))]
        (if (and (< (- px ox) x (+ px ox))
                 (< (- py oy) y (+ py oy)))
          (draw-world c))))
   @players)))

(defn new-player [c]
  (swap! players assoc c {
    :pos [(rand-int 20)(rand-int 20)]
    :window {:w 10 :h 10}
    :color :white
    :background :black})
  (write c esc/naws)
  (write c esc/char-mode)
  (write c esc/no-echo)
  (write c esc/CLR)
  (draw-world c))


(def move-map {
  :arrow-left   [-1 0]
  :arrow-right  [1 0]
  :arrow-up     [0 -1]
  :arrow-down   [0 1]})

(defn index [col v]
  (first (remove nil? (map-indexed (fn [i x] (if (= x v) i)) col))))

(defn map-idx [col i]
  (first (drop i (cycle col))))

(index esc/color-names :white)
(map-idx esc/color-names 1)
(map-idx esc/color-names (inc (index esc/color-names :white)))


(defn change-color [c k n]
  (set-state! c k (map-idx color-names (+ n (index color-names (state c k)))))
  (draw-world c))

(defn player-input [c s] 
  (when-let [delta (move-map s)]
    (update-state! c :pos #(mapv + delta %))
    (apply update-vision (state c :pos)))
  (cond 
    (= s :pageup)   (change-color c :color 1)
    (= s :pagedown) (change-color c :color -1)
    (= s :home)     (change-color c :background 1)
    (= s :end)      (change-color c :background -1))
  (when-not (keyword? s)
    (swap! world assoc (state c :pos) {
      :char (str (code (state c :color)) 
                 (background (state c :background))
                 s (code :reset))})
    (apply update-vision (state c :pos))))

(defn player-quit [c]
  (swap! players dissoc c)
  (prn :disconnect c))

(defn player-resize [c m]
  (let [{:keys [w h]} m]
    (set-state! c :window {:w (min w 120) :h (min h 120)})
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
  (create-server 5072 {
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
