(ns scroll-restore.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [bidi.bidi :as bidi]
            [accountant.core :as accountant]
            [scroll-restore.scrolling :as scrolling]))

(enable-console-print!)

(def app-routes
  ["/" {"" :index
        "section-a" {"" :section-a
                     ["#item-" :item-id] :section-a-deeplink
                     ["/item-" :item-id] :a-item}
        "section-b" :section-b
        "missing-route" :missing-route
        true :four-o-four}])

(defmulti page-contents identity)

(defmethod page-contents :index []
  (fn []
    [:span
     [:h1 "Routing and scroll restore example: Index"]
     [:ul
      [:li [:a {:href (bidi/path-for app-routes :section-a) } "Section A"]
       [:ul
        [:li
         [:a {:href (bidi/path-for app-routes :section-a-deeplink :item-id 75)} "Section A: Item: 75"]]]]
      [:li [:a {:href (bidi/path-for app-routes :section-b) } "Section B"]]
      [:li [:a {:href (bidi/path-for app-routes :missing-route) } "Missing-route"]]
      [:li [:a {:href "/borken/link" } "Borken link"]]]]))

(defn section-a-contents []
  (fn []
    [:span
     [:h1 "Section A"]
     [:ul (map (fn [item-id]
                 [:li {:id (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (bidi/path-for app-routes :a-item :item-id item-id)} "Item: " item-id]])
               (range 1 125))]
     [:a {:href "#item-50"} "Item: 50"]]))

(defmethod page-contents :section-a []
  (section-a-contents))

(defmethod page-contents :section-a-deeplink []
  (section-a-contents))

(defmethod page-contents :a-item []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span
       [:h1 (str "Section A, item " item)]
       [:p [:a {:href (bidi/path-for app-routes :section-a)} "Back to Section A"]]])))

(defmethod page-contents :section-b []
  (fn []
    [:span
     [:h1 "Section B"]]))

(defmethod page-contents :four-o-four []
  "Non-existing routes go here"
  (fn []
    [:span
     [:h1 "404: It is not here"]
     [:pre.verse
      "What you are looking for,
I do not have.
How could I have,
what does not exist?"]]))

(defmethod page-contents :default []
  "Configured routes, missing an implementation, go here"
  (fn []
    [:span
     [:h1 "404: My bad"]
     [:pre.verse
      "This page should be here,
but I never created it."]]))

(defn page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:div#sticky-menu
        [:ul
         [:li [:a {:href (bidi/path-for app-routes :index) } "Home"]]
         [:li [:a {:href (bidi/path-for app-routes :section-a) } "Section A"]]
         [:li [:a {:href (bidi/path-for app-routes :section-b) } "Section B"]]]]
       [:div#page
        ^{:key page} [page-contents page]
        [:p "(Using "
         [:a {:href "https://reagent-project.github.io/"} "Reagent"] ", "
         [:a {:href "https://github.com/juxt/bidi"} "Bidi"] " & "
         [:a {:href "https://github.com/venantius/accountant"} "Accountant"]
         ")"]]])))

(defn on-js-reload []
  (reagent/render-component [page]
                            (. js/document (getElementById "app"))))

(defn ^:export init! []
  (scrolling/initialize! {:sticky-top-element-id "sticky-menu"})
  (accountant/configure-navigation!
   {:nav-handler (fn
                   [path]
                   (let [match (bidi/match-route app-routes path)
                         current-page (:handler match)
                         route-params (:route-params match)]
                     (session/put! :route {:current-page current-page
                                           :route-params route-params})
                     (scrolling/update-navigation! {:path path})))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route app-routes path)))})
  (accountant/dispatch-current!)
  (on-js-reload))
