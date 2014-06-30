(ns modal-ex.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]))

(enable-console-print!)

(def app-state (atom {:modal {:display false
                              :path nil}
                      :columns [{:name "To Do" :cards [{:task "Sidebar"
                                                        :id 0}
                                                       {:task "Header"
                                                        :id 1}
                                                       {:task "Create new board"
                                                        :id 2}]
                                 :state {:add-header? false
                                         :add-card? false
                                         :card-modal {:display false
                                                      :id 10}}}
                                {:name "Doing" :cards [{:task "Drag Lists"
                                                        :id 1}
                                                       {:task "Drag Cards"
                                                        :id 2}
                                                       {:task "Make it look pretty"
                                                        :id 3}
                                                       {:task "Handle Resizing"
                                                        :id 4}
                                                       {:task "placeholder"
                                                        :id 5}
                                                       {:task "placeholder"
                                                        :id 6}
                                                       {:task "placeholder"
                                                        :id 7}
                                                       {:task "placeholder"
                                                        :id 8}
                                                       {:task "placeholder"
                                                        :id 9}
                                                       {:task "placeholder"
                                                        :id 10}
                                                       {:task "placeholder"
                                                        :id 11}
                                                       {:task "placeholder"
                                                        :id 12}]
                                 :state {:add-header? false
                                         :add-card? false
                                         :card-modal {:display false
                                                      :id 0}}}
                                {:name "Done" :cards [{:task "hi"}]
                                 :state {:add-header? false
                                         :add-card? false
                                         :card-modal {:display false
                                                      :id 0}}}]}))

(defn card-component [data owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [c-click]}]
      (let [path (.. data -path)]
        (dom/div #js {:onClick #(put! c-click data)} (:task data))))))

(defn main-component [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:c-click (chan)})

    om/IWillMount
    (will-mount [_]
      (let [c-click (om/get-state owner :c-click)]
        (go (while true
              (let [card (<! c-click)]
                (om/update! data :modal {:card card}))))))

    om/IRenderState
    (render-state [_ {:keys [c-click]}]
      (apply dom/div nil
        (map
          #(apply dom/div nil
             (om/build-all card-component (:cards %) {:state {:c-click c-click}}))
          (:columns data))))))

(om/root
  main-component
  app-state
  {:target (. js/document (getElementById "app"))})


(defn display-modal? [data]
  (if (nil? (:card data))
    #js {:display "none"}
    #js {:display "block"}))

(defn modal-text [data owner]
  (reify
    om/IRender
    (render [_]
      (let [task (:task data)]
        (dom/div #js {:style #js {:width 200
                                  :height 200
                                  :position "absolute"
                                  :left "50%"
                                  :top "50%"
                                  :margin-top "-100px"
                                  :margin-left "-100px"
                                  :background-color "white"}}
          (dom/input #js {:onChange #(let [new-val (.. % -target -value)]
                                       (om/transact! data :task (fn [_] new-val)))
                          :defaultValue task}))))))

(defn modal-component [data owner]
  (reify
    om/IRender
    (render [_]
      (let [modal (:modal data)]
        (dom/div #js {:style (display-modal? modal)}
          (when (:card modal)
            (dom/div nil
              (dom/div #js {:className "overlay"
                            :style #js {:width "100%"
                                        :height "100%"
                                        :background-color "#ccc"
                                        :position "fixed"
                                        :top 0
                                        :left 0
                                        :opacity "0.7"}
                            :onClick #(om/update! data :modal {:path card})})
              (dom/div nil
                (om/build modal-text (get-in data [:modal :card]))))))))))

(om/root
  modal-component
  app-state
  {:target (. js/document (getElementById "modal"))})
