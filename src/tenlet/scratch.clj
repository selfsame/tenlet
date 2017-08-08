(ns tenlet.scratch
  (:use tenlet.server))

'(. server (close))

;echo
(send! T_IAC T_WONT T_ECHO)
;no echo
(send! T_IAC T_WILL T_ECHO)

;enter char mode
(send! T_IAC T_DO T_LINE)


(send! T_IAC T_DONT T_LINE)

;not sure
(send! ansi-esc T_ORIG )

;request screen size reports
(send! T_IAC T_DO T_NAWS)

;clear screen
(send! T_CLR)

;hide cursor
(send! T_CSI T_HIDE)


(for [i (range 20)]
  (do 
    (send! (cursor i i))
    (send! "@")))