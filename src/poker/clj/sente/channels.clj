(ns poker.clj.sente.channels
  (:require [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]
            [clj-uuid :as uuid]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(defn uuid-ignore-request [_] (str (uuid/v1)))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {:user-id-fn #'uuid-ignore-request})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )
