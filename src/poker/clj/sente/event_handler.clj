(ns poker.clj.sente.event-handler
  (:require
   [poker.clj.rooms :refer (join-room apply-vote drop-user reveal-vote)]
   [poker.clj.sente.channels :refer [connected-uids chsk-send!]]
   [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(defmulti event
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defmethod event
  :default ; Default/fallback case (no other matching handler)
  [{:keys [event ring-req ?reply-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (debugf "Unhandled event: %s, uid: %s" event, uid)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event :chsk/uidport-open [{:keys [uid client-id]}]
  (println "New connection:" uid client-id))

(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (drop-user uid)
  (println "Disconnected:" uid))

(defmethod event :chsk/handshake [{:keys [?data]}]
  (println "Handshake:" ?data))

(defmethod event :chsk/ws-ping [_])

(defmethod event :poker/join-room [{:as ev-msg :keys [?data]}]
  (join-room (:uid ev-msg) (:roomname ?data) (:username ?data))
  (println (format "%s joined room %s" (:username ?data) (:roomname ?data))))

(defmethod event :poker/leave [{:keys [uid]}]
  (drop-user uid)
  (println "Disconnected:" uid))

(defmethod event :poker/vote [{:as ev-msg :keys [?data]}]
  (apply-vote (:uid ev-msg) (:vote ?data)))

(defmethod event :poker/request-reveal [{:as ev-msg}]
  (reveal-vote (:uid ev-msg)))
