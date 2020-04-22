(ns poker.cljs.components.room-component
  (:require [clojure.string]
            [poker.cljs.localstorage :as localstorage]
            [poker.cljs.sente.router :as router]))

(defn members-list [props]
  [:div.panel [:p (:user-id props)] [:p "member 2"]])

(defn vote-controls [props]
  [:div.panel [:button "0"] [:button "1"] [:button "2"] [:button "3"] [:button "?"]])

(defn message [props]
  [:p "its a tie! Fight to the death"])

(defn history [props]
  [:div.panel [:p  "1"] [:p "2"]])

(defn room-component []
(let [user-id (localstorage/get-item :user-id)]
(if (clojure.string/blank? user-id)
[:p "user-id not set"]
(do (router/start-router!) (fn []
  [:div.room
  [members-list {:user-id user-id}]
  [vote-controls]
  [message]
  [history]])
))))
