(ns poker.cljs.components.room-component
  (:require [clojure.string]
            [reagent.core :as r]
            [poker.cljs.sente.event-handler :refer (send-vote request-reveal)]
            [poker.cljs.localstorage :as localstorage]
            [poker.cljs.sente.router :as router]))

(def room-component-state (r/atom {:users [{:username "el bronco" :voted true} {:username "rufus" :voted false}] :history [[1 1 1 2] [2 2 1 2] [1] [2 3 3] [1 2 3 4] [1 "?" 3]] :local-vote nil}))

(defn members-list []
  [:div.member-list
   (for [user (:users @room-component-state)]
     (let [username (:username user)
           vote-mark (if (:voted user) " ✓" "")]
       ^{:key username} [:div.member [:p username vote-mark]]))])

(defn set-and-send-vote [vote]
  (swap! room-component-state assoc :local-vote vote)
  (send-vote vote))

(defn vote-controls [props]
  (let [local-vote (:local-vote @room-component-state)]
    [:div.panel [(if (= local-vote 0) :button.active :button) {:on-click #(set-and-send-vote 0)} "0"]
     [(if (= local-vote 1) :button.active :button) {:on-click #(set-and-send-vote 1)} "1"]
     [(if (= local-vote 2) :button.active :button) {:on-click #(set-and-send-vote 2)} "2"]
     [(if (= local-vote 3) :button.active :button) {:on-click #(set-and-send-vote 3)} "3"]
     [(if (= local-vote "?") :button.active :button) {:on-click #(set-and-send-vote "?")} "?"]]))

(defn message [props]
  [:p "its a tie! Fight to the death"])

(defn set-and-request-reveal []
  (swap! room-component-state assoc :local-vote vote)
  (request-reveal))

(defn reveal-button []
  [:div.panel [:button {:on-click #(set-and-request-reveal)} "Reveal"]])

(defn history [props]
  [:div.results-list
   [:h3 "previous results"]
   (for [result (map-indexed (fn [x y] [x y]) (:history @room-component-state))]
     ^{:key (first result)} [:div.result {:style {:opacity (- 1.0 (/ (first result) 5.0))}} (clojure.string/join ", " (second result))])])

(defn room-component []
  (let [user-id (localstorage/get-item :user-id)]
    (if (clojure.string/blank? user-id)
      [:p "user-id not set"]
      (do (router/start-router!) (fn []
                                   [:div.room
                                    [members-list {:user-id user-id}]
                                    [vote-controls]
                                    [reveal-button]
                                    [message]
                                    [history]])))))
