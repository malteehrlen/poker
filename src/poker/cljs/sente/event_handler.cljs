(ns poker.cljs.sente.event-handler
  (:require [poker.cljs.sente.channels :refer (chsk-send!)]
            [poker.cljs.components.room-state :refer (room-state)]
            [poker.cljs.localstorage :refer (get-user-id get-room-id)]))

(defmulti -event-msg-handler
  :id)

(defn event-msg-handler
  [{:as ev-msg}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default
  [{:keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state [{:keys [?data]}]
  (if (:first-open? (second ?data))
    (println "Channel socket successfully established!")))

(defmethod -event-msg-handler :chsk/recv
  [{:keys [?data]}]
  (reset! room-state (second ?data)))

(defn join-room []
  (chsk-send! [:poker/join-room {:username (get-user-id) :roomname (get-room-id)}]))

(defn leave-room []
  (chsk-send! [:poker/leave]))

(defmethod -event-msg-handler :chsk/handshake []
  (join-room))


(defn send-vote [vote]
  (chsk-send! [:poker/vote {:vote vote}]))

(defn request-reveal []
  (chsk-send! [:poker/request-reveal]))
