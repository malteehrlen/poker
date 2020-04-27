(ns poker.cljs.components.room-component
  (:require [clojure.string]
            [poker.cljs.components.room-state :refer (room-state)]
            [poker.cljs.sente.event-handler :refer (send-vote request-reveal)]
            [poker.cljs.localstorage :as localstorage]
            [poker.cljs.sente.router :as router]))

(defn members-list []
  [:div.member-list
   (for [user (:room-state @room-state)]
     (let [username (:username user)
           vote-mark (if (:vote user) " ✓" "")]
       ^{:key username} [:div.member [:p (str username) vote-mark]]))])

(defn set-and-send-vote [vote]
  (swap! room-state assoc :local-vote vote)
  (send-vote vote))

(defn vote-controls [props]
  (let [local-vote (:local-vote @room-state)]
    [:div.panel [(if (= local-vote 0) :button.active :button) {:on-click #(set-and-send-vote 0)} "0"]
     [(if (= local-vote 1) :button.active :button) {:on-click #(set-and-send-vote 1)} "1"]
     [(if (= local-vote 2) :button.active :button) {:on-click #(set-and-send-vote 2)} "2"]
     [(if (= local-vote 3) :button.active :button) {:on-click #(set-and-send-vote 3)} "3"]
     [(if (= local-vote "?") :button.active :button) {:on-click #(set-and-send-vote "?")} "?"]]))

(defn message [props]
  [:p "its a tie! Fight to the death"])

(defn set-and-request-reveal []
  (swap! room-state assoc :local-vote vote)
  (request-reveal))

(defn reveal-button []
  [:div.panel [:button {:on-click #(set-and-request-reveal)} "Reveal"]])

(defn history [props]
  [:div.results-list
   [:h3 "previous results"]
   (for [result (map-indexed (fn [x y] [x y]) (:history @room-state))]
     ^{:key (first result)} [:div.result {:style {:opacity (- 1.0 (/ (first result) 5.0))}} (clojure.string/join ", " (second result))])])

(defn room-component []
  (let [user-id (localstorage/get-item :user-id)]
    (if (clojure.string/blank? user-id)
      [:p "user-id not set"]
      (do (router/start-router!) (fn []
                                   [:div.room
                                    [members-list]
                                    [vote-controls]
                                    [reveal-button]
                                    [message]
                                    [history]])))))
