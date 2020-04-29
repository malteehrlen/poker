(ns poker.cljs.client
  (:require
   [accountant.core :as accountant]
   [clerk.core :as clerk]
   [poker.cljs.components.room-component :as room-component :refer [room-component]]
   [poker.cljs.localstorage :as localstorage :refer (get-room-id get-user-id)]
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

(defn home-page []
  (let [room-id (atom (get-room-id))
        user-id (atom (get-user-id))]
    (fn []
      [:span.main {}
       [:h1 "P O K E R M A N C E R"]
       [:div.content
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
        [:button {:on-click #(handle-room-open @room-id @user-id)} "start pokering"]]])))

(defn handle-room-open [room-id user-id]
  (localstorage/set-item! :room-id room-id)
  (localstorage/set-item! :user-id user-id)
  (accountant/navigate! (path-for :room {:room-id room-id})))

(defn room-page []
  (let [routing-data (session/get :route)
        room-id (get-in routing-data [:route-params :room-id])]
    ;; if the user is landing on this url from outside
    (localstorage/set-item! :room-id room-id)
    (if (nil? (localstorage/get-item :user-id)) (accountant/navigate! (path-for :index)))
    (fn []
      [:span.main
       [:div.content
        [:h1 (str room-id)]
        [room-component]
        [:p.back [:a {:href (path-for :index)} "Back to the lobby"]]]])))

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
