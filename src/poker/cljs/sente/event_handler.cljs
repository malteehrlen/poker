(ns poker.cljs.sente.event-handler
  (:require [poker.cljs.sente.channels :refer (chsk-send!)]
            [poker.cljs.localstorage :refer (get-user-id get-room-id)]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state [{:as ev-msg :keys [?data]}]
  (if (:first-open? (second ?data))
    (println "Channel socket successfully established!")
    (println "Channel socket state change:" (second ?data))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (println "Push event from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (join-room)
    (println "Handshake: %s" ?data)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

(defn join-room []
  (chsk-send! [:poker/join-room {:username (get-user-id) :roomname (get-room-id)}]))

(defn send-vote [vote]
  (chsk-send! [:poker/vote {:vote vote}]))

(defn request-reveal []
  (chsk-send! [:poker/request-reveal]))
