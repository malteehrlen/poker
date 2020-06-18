(ns poker.cljs.components.room-component
  (:require [clojure.string]
            [reagent.core :as r]
            [poker.cljs.components.room-state :refer (room-state)]
            [poker.cljs.sente.event-handler :refer (send-vote request-reveal)]
            [poker.cljs.localstorage :as localstorage]
            [poker.cljs.sente.router :as router]))

(def local-room-state (r/atom {}))
(def round-count (atom 0))

(defn members-list []
  [:div.member-list
   (doall (map-indexed (fn [index user]
                         (let [username (:username user)
                               vote-mark (if (:vote user) " ✓" "")]
                           ^{:key index} [:div.member [:p (str username) vote-mark]])) (:room-state @room-state)))])

(defn set-and-send-vote [vote]
  (swap! local-room-state assoc :local-vote vote)
  (send-vote vote))

(defn vote-controls [] (let [local-vote (:local-vote @local-room-state)]
                         [:div.panel [(if (= local-vote 0) :button.active :button) {:on-click #(set-and-send-vote 0)} "0"]
                          [(if (= local-vote 1) :button.active :button) {:on-click #(set-and-send-vote 1)} "1"]
                          [(if (= local-vote 2) :button.active :button) {:on-click #(set-and-send-vote 2)} "2"]
                          [(if (= local-vote 3) :button.active :button) {:on-click #(set-and-send-vote 3)} "3"]
                          [(if (= local-vote "?") :button.active :button) {:on-click #(set-and-send-vote "?")} "?"]]))

(defn set-and-request-reveal []
  (swap! local-room-state dissoc :local-vote)
  (request-reveal))

(defn everyone-voted? []
  (every? true? (->> @room-state
                     (:room-state)
                     (map :vote))))

(defn reveal-button []
  (let [button-div (if (everyone-voted?) :button.ready :button)]
    [:div.panel [button-div {:on-click #(set-and-request-reveal)} "Reveal"]]))

(defn history []
  (let [current-round (count (:vote-history @room-state))]
    (if (> current-round @round-count) (do (swap! local-room-state dissoc :local-vote) (reset! round-count current-round)))
    [:div.results-list
     [:h3 "Results"]
     (for [result (map-indexed (fn [x y] [x y]) (:vote-history @room-state))]
       (do
         ^{:key (first result)} [:div.result {:style {:opacity (- 1.0 (/ (first result) 5.0))}} (clojure.string/join ", " (remove nil? (second result)))]))]))

(defn room-component []
  (let [user-id (localstorage/get-item :user-id)]
    (if (clojure.string/blank? user-id)
      [:p "user-id not set"]
      (do (router/start-router!) (fn []
                                   [:div.room
                                    [vote-controls]
                                    [reveal-button]
                                    [members-list]
                                    [history]])))))
