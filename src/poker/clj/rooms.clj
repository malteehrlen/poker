(ns poker.clj.rooms
  (:require [digest :refer (md5)]))

(defn safe-k [s] (keyword (md5 s)))

(def rooms (atom {}))
(def users (atom {}))

(defn join-room [uid roomname username]
  (let [room-k (safe-k roomname)
        uid-k (keyword uid)]
    (swap! rooms assoc-in [room-k uid-k] {:username username :vote nil})
    (swap! users assoc uid-k room-k)
    (println @rooms)))

(defn drop-user [uid]
  (swap! rooms update-in [((keyword uid) @users)] dissoc (keyword uid))
  (swap! users dissoc (keyword uid))
  (println @rooms))

(defn apply-vote [uid vote roomname]
  (swap! rooms assoc-in [(safe-k roomname) (keyword uid) :vote] vote))

