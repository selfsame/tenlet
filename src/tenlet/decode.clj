(ns tenlet.decode
  (:use 
    pdfn.core)
  (:require
    [clojure.pprint :as pprint]))

;http://www.asciitable.com/


(def ETX        (char 3))
(def EOT        (char 4))
(def STX        (char 2))
(def SI         (char 15))
(def ENQ        (char 5))
(def BEL        (char 7))
(def FS         (char 28))
(def BACKSPACE  (char 8))
(def SYN        (char 22))
(def DC1        (char 17))
(def DLE        (char 16))
(def DC3        (char 19))
(def NAK        (char 21))
(def ETB        (char 23))
(def VT         (char 11))

(def IAC?     (is* \ÿ))
(def ESC?     (is* (char 27)))
(def NULL?    (is* (char 0)))
(def NAWS?    (is* (char 31)))
(def SB?      (is* (char 250)))
(def SE?      (is* (char 240)))
(def LB?      (is* \[ ))
(def TILDE?   (is* \~ ))
(def IAC-END? #{\ð})
(def IGNORED? #{})
(def DISPLAY? #(> (int %) 31))
(def RETURN?  (is* \return))

(defpdfn ^{:inline true} op)


(pdfn op [^empty? col c]
  {})

(pdfn op [col ^DISPLAY? c]
  (-> col 
    (assoc :out c) 
    (update :chars conj c) 
    (update :chars vec)))

(pdfn op [col ^RETURN? c]
  (-> col 
    (assoc :out c) 
    (assoc :line (apply str (:chars col))) ))

(pdfn op [col ^ESC? c]
  (assoc col :esc true))

(pdfn op [^:iac col c]
  (update col :chars conj c))

(pdfn op [^:iac col ^SB? c]
  (-> col
    (assoc :sb true)))

(pdfn op [^:sb col ^NAWS? c]
  {:naws true 
   :chars []})

(pdfn op [^:iac col ^IAC-END? c]
  {:out (conj (:chars col) c)})

(pdfn op [col ^IAC? c]
  {:iac true
   :chars []})

(pdfn op [col ^NULL? c]
  {})

(pdfn op [^:naws col c]
  (let [col (update col :chars conj c)
        chars (:chars col)]
    (if (= 6 (count chars))
      (let [[w1 w2 h1 h2 iac se] (mapv int chars)
            w (+ (bit-shift-left w1 8) w2)
            h (+ (bit-shift-left h1 8) h2)]
        (if (and (IAC? (char iac)) (SE? (char se)))
          {:resize {:w w :h h}}
          {:error "Could not negotiate NAWS"}))
      col)))

(pdfn op [^:esc col ^LB? c]
  (assoc col :code true))

(pdfn op [^:code col c]
  (if-let [found 
    (case c
      \A :arrow-up
      \B :arrow-down
      \D :arrow-left
      \C :arrow-right
      \F :end
      \H :home
      \E :numpad-5
      nil)]
    (-> col 
      (dissoc :esc)
      (dissoc :code)
      (assoc :out found))
    (-> col
      (update :codes vec)
      (update :codes conj c))))

(pdfn op [^:code col ^TILDE? c]
  (if-let [found 
    (case (:codes col)
      [\2] :insert
      [\3] :delete
      [\5] :pageup
      [\6] :pagedown
      [\1 \5] :f5
      [\1 \7] :f6
      [\1 \8] :f7
      [\1 \9] :f8
      [\2 \0] :f9
      [\2 \1] :f10
      [\2 \3] :f11
      [\2 \4] :f12
      nil)]
    (-> col 
      (dissoc :esc)
      (dissoc :code)
      (dissoc :codes)
      (assoc :out found))
    (-> col 
      (dissoc :esc)
      (dissoc :code)
      (dissoc :codes))))

(inspect op)

