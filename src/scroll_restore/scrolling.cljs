(ns ^{:doc "Navigation scroll restoring for clojurescript SPA projects.

  A scroll watcher keeps each entry on the history stack updated with the scroll
  position for the active entry. When the user is navigating to existing history entries
  scroll position is restored.

  New navigation history entries are always scrolled to the hash target in the location,
  or, when the target is missing, the top of the page.

  Unfortunately only Chrome supports a reliable way to disable the browser's scroll
  restoration, but at least there we can create a smooth scroll restoration experience."}
    scroll-restore.scrolling
  (:require
   [goog.events :as events]
   [goog.events.EventType :as EventType]
   [goog.fx.dom :as fx-dom]
   [goog.dom :as dom]
   [goog.style :as style]
   [goog.functions :refer [debounce]])
  (:import goog.Uri))

(defonce navigation-info (atom {:path nil :hash nil}))
(defonce restoring? (atom false))

(defn- get-history-state
  "Stuff we want to remember when the user navigates back and forths in
  the history stack are saved as state in each history entry."
  []
  (.-state js/history))

(defn- get-history-scroll-top
  "If we have a saved scroll-top in the history state return it,
   otherwise return nil"
  []
  (when-let [state (get-history-state)]
    (aget state "scroll-top")))

(defn get-scroll-top
  "Current scroll position"
  []
  (.-y (dom/getDocumentScroll)))

(defn- scroll-to
  "Scroll immediatelly to `y`"
  [y]
  (js/scrollTo 0 y))

(defn- smooth-scroll-to
  "Scroll to `y` with some easing"
  [y]
  (.play (fx-dom/Scroll. (dom/getDocumentScrollElement) (clj->js [0 (get-scroll-top)]) (clj->js [0 y]) 300)))

(defn top-of-element-with-id [id]
  (if-let [element (dom/getElement id)]
    (style/getPageOffsetTop element)
    0))

(defn- bottom-of-element-with-id [id]
  (if-let [element (dom/getElement id)]
    (+ (.-y (style/getPosition element)) (.-height (style/getSize element)))
    0))

(def browser-supports-manual-restoration? (.-scrollRestoration js/history))

(defn- disable-default-scroll-restoration
  "The browser's default behaviour is to restore scroll position on revisit of a
  history stack entry (but it doesn't really handle it well for SPA:s.)
  Chrome supports to disable it. Other browsers don't really,
  but there's no visible penalty for trying, so we try."
  []
  (if browser-supports-manual-restoration?
    (set! (.-scrollRestoration js/history) "manual")
    (events/listen
     js/window EventType/POPSTATE
     (fn [event]
       (let [state (.-state event)]
         (when-let [scroll-top (aget state "scroll-top")]
           (events/listenOnce
            js/window EventType/SCROLL
            (fn []
              (js/scrollTo 0 scroll-top)
              nil))))
       nil))))

(defn- install-scroll-saver
  "When the user scrolls we save the current scroll position in the
  current history entry's state."
  []
  (events/listen
   js/window EventType/SCROLL
   (debounce
    (fn [event]
      (let [scroll-top (get-scroll-top)
            state {:scroll-top scroll-top}]
        (.replaceState js/history (clj->js state) (.-title js/document)))
      nil)
    200)))

(defn- install-scroll-restorer
  "When the user navigates to an entry present in the history stack we
  pick the saved scroll position from the event's history object's state, then:
  1. wait for navigation info to change
  2. wait for rendering to be done
  3. Restore the saved scrolled position."
  []
  (events/listen
   js/window EventType/POPSTATE
   (fn [event]
     (let [state (.-state event)]
       (when-let [scroll-top (when state (aget state "scroll-top"))]
         (reset! restoring? true)
         (js/requestAnimationFrame
           (fn []
             (js/requestAnimationFrame
               #(do (reset! restoring? false)
                 (scroll-to scroll-top)))))))
     nil)))

(defn- install-navigation-watcher
  "When navigation is dispatched it could either be to an existing instance on the
  history stack, moving back an forth in the navigation history. If so we let it be
  since the scroll restorer will take care of that.
  Or it can be a freshly created history instance, typically as a result of
  interacting with our app. Then it's our responsibility to scroll and we wait
  for rendering to be finsished before doing it."
  [sticky-top-element-id]
  (add-watch
   navigation-info :navigation-watcher
   (fn [key navigation-info old-nav new-nav]
     (let [old-path (:path old-nav)
           new-path (:path new-nav)
           new-hash (.getFragment (Uri. new-path))
           saved-scroll-top (get-history-scroll-top)
           same-page? (= (.getPath (Uri. old-path)) (.getPath (Uri. new-path)))]
       (when (:path old-nav) ;; No scrolling on initial load of the SPA
         (when-not @restoring?
           (js/requestAnimationFrame
            #(let [hash-target-top (top-of-element-with-id new-hash)
                   top-element-bottom (if sticky-top-element-id (bottom-of-element-with-id sticky-top-element-id) 0)
                   top (cond (some? saved-scroll-top) saved-scroll-top
                             (some? hash) (- hash-target-top top-element-bottom)
                             :else 0)]
               (js/console.log "scroll: " top)
               (if same-page?
                 (smooth-scroll-to top)
                 (scroll-to top))))))))))

(defn update-navigation!
  "To be called when the user's navigation intent has been dispatched.
  `new-nav` should be a map like so: `{:path new-path :hash new-hash}`"
  [new-nav]
  (reset! navigation-info new-nav))

(defn initialize!
  "Call when your app is starting."
  ([]
   (initialize! nil))
  ([{:keys [sticky-top-element-id]}]
   (install-navigation-watcher sticky-top-element-id)
   (disable-default-scroll-restoration)
   (install-scroll-restorer)
   (install-scroll-saver)))
