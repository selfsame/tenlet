(ns tenlet.scratch
  (:require 
    [tenlet.server :refer [write close create-server DEBUG]]
    [tenlet.escape :as esc :refer [code background color-names]]
  #?(:cljs [cljs.nodejs :as nodejs])))

#?(:cljs (def fs (nodejs/require "fs")))
#?(:cljs (defn spit [f s] (fs.writeFileSync f (str s))))
#?(:cljs (defn slurp [f] (.toString (fs.readFileSync f))))

(def players (atom {}))

(defn load-world []
#?(:clj  
  (try 
    (read-string (slurp "world.edn"))
    (catch Exception e {}))
 :cljs 
  (try 
    (cljs.reader/read-string (slurp "world.edn"))
    (catch :default e {}))))

(defonce world (atom (load-world)))

(defn save-world []
  (spit "world.edn" @world))

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

(defn write* [c & more]
  (update-state! c ::write #(apply str (cons % more))))

(defn flush* [c]
  (write c (state c ::write))
  (set-state! c ::write ""))

(defn within? [c x y]
  (let [[px py] (state c :pos)
        {:keys [w h]} (state c :window)
        ox (int (* w 0.5))
        oy (int (* h 0.5))]
    (if (and (< (- px ox) x (+ px ox))
             (< (- py oy) y (+ py oy)))
        true)))

(defn each-player [f]
  (dorun (map
    (fn [[c m]] (f c))
   @players)))

(defn screen-space [c x2 y2]
  (let [{:keys [w h]} (state c :window)
        [x y] (state c :pos)
        ox (int (* w 0.5))
        oy (int (* h 0.5))]
    [(+ (- x2 x) ox)
     (+ (- y2 y) oy)]))

(defn draw-world [c]
  (let [{:keys [w h]} (state c :window)
        [x y] (state c :pos)
        ox (int (* w 0.5))
        oy (int (* h 0.5))
        world @world]
    (write* c esc/CLR)
    (dorun 
      (for [xx (range w)
            yy (range h)
            :let [x2 (- (+ xx x) ox)
                  y2 (- (+ yy y) oy)
                  tile (get world [x2 y2])]
            :when tile]
      (write* c 
        (esc/cursor xx yy)
        (:char tile))))
    (write* c 
      (esc/cursor 1 (- h 1))
      (apply str (take w (repeat \-)))
      (esc/cursor 1 h)
      (apply str (take w (repeat " ")))
    
      (esc/cursor 1 h)
      "color <" (code (state c :color)) "#" (code :reset)
      "> (pageup)    "
      "background <" (background (state c :background)) " " (code :reset)
      "> (pagedown)   quit (f1)"
      (esc/cursor (- w 10) h)
      (code :green) "(" x " " y ")   " (code :reset))
    (each-player 
      (fn [o] 
        (let [[x2 y2] (state o :pos)
              [ox2 oy2] (screen-space c x2 y2)]
          (if (within? c x2 y2)
            (write* c
              (code (state o :color))
              (background (state o :background))
              (esc/cursor ox2 oy2) "@"
              (code :reset))))))
    (flush* c)))

(defn update-vision [x y]
  (each-player (fn [c] (if (within? c x y) (draw-world c)))))

(defn new-player [c]
  (save-world)
  (swap! players assoc c {
    :pos [(rand-int 20)(rand-int 20)]
    :window {:w 10 :h 10}
    :color :white
    :background :black})
  (write c esc/naws)
  (write c esc/char-mode)
  (write c esc/no-echo)
  (write c esc/CLR)
  (write c esc/hide-cursor)
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
    (= s :pagedown) (change-color c :background 1)
    (= s :f1)       (do (write c esc/show-cursor)
                        (write c esc/CLR)
                        (close c)))
  (when-not (keyword? s)
    (swap! world assoc (state c :pos) {
      :char (str (code (state c :color)) 
                 (background (state c :background))
                 s (code :reset))})
    (apply update-vision (state c :pos))))

(defn player-quit [c]
  (swap! players dissoc c)
  (prn :disconnect c)
  (save-world))

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