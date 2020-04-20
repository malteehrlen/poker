(ns poker.client
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/:room-id" :room]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(defn home-page []
  (let [room-name (atom "")
        user-id (atom "")]
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
       :value @room-name
       :on-change    #(reset! room-name (.. % -target -value))}]
      [:input 
      {:type "button"
       :value "start pokering"
       :on-click #(accountant/navigate! (path-for :room {:room-id @room-name}))}]]])))


(defn room-page []
  (fn []
    (let [routing-data (session/get :route)
          room-id (get-in routing-data [:route-params :room-id])]
      [:span.main
       [:h1 (str room-id)]
       [:p [:a {:href (path-for :index)} "Back to the lobby"]]])))

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
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(init!)
