(ns hub.girouette
  (:require
    [clojure.string :as str]
    [garden.core :as garden]
    [girouette.tw.core :refer [make-api]]
    [girouette.util :as util]
    [girouette.tw.common :as common]
    [girouette.tw.color :as color]
    [girouette.tw.layout :as layout]
    [girouette.tw.flexbox :as flexbox]
    [girouette.tw.grid :as grid]
    [girouette.tw.box-alignment :as box-alignment]
    [girouette.tw.spacing :as spacing]
    [girouette.tw.sizing :as sizing]
    [girouette.tw.typography :as typography]
    [girouette.tw.background :as background]
    [girouette.tw.border :as border]
    [girouette.tw.effect :as effect]
    [girouette.tw.table :as table]
    [girouette.tw.animation :as animation]
    [girouette.tw.transform :as transform]
    [girouette.tw.interactivity :as interactivity]
    [girouette.tw.svg :as svg]
    [girouette.tw.accessibility :as accessibility]
    [hub.css :as hub]))

(def components
  (util/into-one-vector
    [accessibility/components
     animation/components
     background/components
     border/components
     box-alignment/components
     common/components
     effect/components
     flexbox/components
     grid/components
     interactivity/components
     layout/components
     sizing/components
     spacing/components
     svg/components
     table/components
     transform/components
     typography/components
     hub/components]))

(def class-name->garden
  (:class-name->garden
    (make-api components
      {:color-map (merge color/default-color-map hub/color-map)
       :font-family-map typography/default-font-family-map})))

(comment
  (class-name->garden ""))
