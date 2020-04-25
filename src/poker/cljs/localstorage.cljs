(ns poker.cljs.localstorage)

(defn set-item!
    "Set `key' in browser's localStorage to `val`."
      [key val]
        (.setItem (.-localStorage js/window) key val))

(defn get-item
    "Returns value of `key' from browser's localStorage."
      [key]
        (.getItem (.-localStorage js/window) key))

(defn remove-item!
    "Remove the browser's localStorage value for the given `key`"
      [key]
        (.removeItem (.-localStorage js/window) key))

(defn get-user-id []
  (or (get-item :user-id) ""))

(defn get-room-id []
  (or (get-item :room-id) ""))
