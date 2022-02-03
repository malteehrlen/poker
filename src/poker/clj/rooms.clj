(ns poker.clj.rooms
  (:require [digest :refer (md5)]
            [poker.clj.sente.channels :refer (chsk-send!)]))

(defn safe-k [s] (keyword (md5 s)))

(def rooms (atom {}))
(def users (atom {}))
(def vote-history (atom {}))

(defn hide-votes [[_ user]]
  (if (nil? (:vote user))
    (assoc user :vote false)
    (assoc user :vote true)))

(defn push-room-update [room-k]
  (doseq [k (keys (room-k @rooms))]
    (chsk-send! (name k) [:poker/room-state {:room-state (map hide-votes (room-k @rooms)) :vote-history (room-k @vote-history)}])))

(defn push-all-rooms []
  (doseq [k (keys @rooms)]
    (push-room-update k)))

(defn join-room [uid roomname username]
  (let [room-k (safe-k roomname)
        uid-k (keyword uid)]
    (swap! rooms assoc-in [room-k uid-k] {:username username :vote nil})
    (swap! users assoc uid-k room-k)
    (push-room-update room-k)))

(defn drop-user [uid]
  (let [room-k ((keyword uid) @users)]
    (if-not (nil? room-k)
      (do
        (swap! rooms update-in [room-k] dissoc (keyword uid))
        (push-room-update room-k)
        (if (empty? (keys (room-k @rooms)))
          (swap! vote-history dissoc room-k))
        (swap! users dissoc (keyword uid))))))

(defn apply-vote [uid vote]
  (let [room-k ((keyword uid) @users)]
    (swap! rooms assoc-in [room-k (keyword uid) :vote] vote)
    (push-room-update room-k)))

(defn reveal-vote [uid]
  (let [room-k ((keyword uid) @users)
        current-votes (map (fn [[_ v]] (:vote v)) (room-k @rooms))]
    (if-not (every? nil? current-votes)
      (do
        (swap! vote-history assoc room-k (conj (room-k @vote-history) current-votes))
        (doseq [user (keys (room-k @rooms))]
          (swap! rooms update-in [room-k user] dissoc :vote))
        (push-room-update room-k)))))
