(ns poker.cljs.client
  (:require [accountant.core :as accountant]
   [clerk.core :as clerk]
   [poker.cljs.components.room-component :as room-component :refer [room-component]]
   [poker.cljs.localstorage :as localstorage :refer (get-room-id get-user-id)]
   [poker.cljs.sente.event-handler :refer (leave-room join-room)]
   [poker.cljs.sente.router :as router]
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [reitit.frontend :as reitit]))

;; -------------------------
;; Routes

(def ring-router
  (reitit/router
   [["/" :index]
    ["/room/:room-id" :room]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name ring-router route params))
    (:path (reitit/match-by-name ring-router route))))

;; -------------------------
;; Page components

(defn handle-room-open [room-id user-id]
  (do
  (localstorage/set-item! :room-id room-id)
  (localstorage/set-item! :user-id user-id)
  (accountant/navigate! (path-for :room {:room-id room-id}))))

(defn home-page []
  (let [room-id (atom (get-room-id))
        user-id (atom (get-user-id))]
    (fn []
      [:span.main {}
       [:h1 "P O K E R M A N C E R"]
       [:div.content
        [:form
        [:input
         {:type "form"
          :placeholder "nickname"
          :value @user-id
          :on-change    #(reset! user-id (.. % -target -value))}]
        [:input
         {:type "form"
          :placeholder "room"
          :value @room-id
          :on-change    #(reset! room-id (.. % -target -value))}]
        [:button {:type "submit" :on-click #(handle-room-open @room-id @user-id)} "start pokering"]]
        [:div.description
        [:p.description "Pokermancer is a story poker tool for scrum teams. Unlike the vast majority of other poker tools, it isnt overdesigned or unstable. Pokermancer doesnt require any sso or signup."] 
        [:p.description "To get started, join a room and share the url with your colleagues. There is no configurability at the moment, so if you are missing vital features feel free to shoot me an email (address below)."]]
          [:p.back 
           [:a {:href "https://www.paypal.com/donate/?hosted_button_id=EJ6PDT5HB8Q4C" :target "_blank" :rel "noopener noreferrer"} "Donate if you want"] 
           [:br] "Written by malte@ehrlen.com" ]
        ]])))

(defn room-page []
  (let [routing-data (session/get :route)
        room-id (get-in routing-data [:route-params :room-id])]
    (do
      (localstorage/set-item! :room-id room-id)
      (if (nil? (localstorage/get-item :user-id)) (accountant/navigate! (path-for :index)))
      (router/start-router!)
      (join-room)
      (fn []
        [:span.main
         [:div.content
          [:h1 (str room-id)]
          [room-component]
          [:p.back 
           [:a {:on-click #(leave-room) :href (path-for :index)} "Back to the lobby"][:br] 
           [:a {:href "https://www.paypal.com/donate/?hosted_button_id=EJ6PDT5HB8Q4C" :target "_blank" :rel "noopener noreferrer"} "Donate if you want"] [:br] "Written by malte@ehrlen.com" ]]]))))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :room #'room-page))


;; -------------------------
;; Page mounting component


(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header]
       [page]
       [:footer]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path ring-router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path ring-router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(init!)
