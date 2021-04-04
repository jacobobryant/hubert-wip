(ns hub.css
  (:require
    [clojure.string :as str]
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
    [girouette.tw.accessibility :as accessibility]))

(declare class-name->garden)

(def apply-classes
  '{btn [text-center py-2 px-4 bg-dark text-white
         rounded disabled:opacity-50]
    btn-hover [bg-black]
    btn-disabled [opacity-50]
    })

(def apply-components
  (vec
    (for [[from to] apply-classes]
      {:id (keyword from)
       :rules (str "\n" from " = <'" from "'>\n")
       :garden (fn [_]
                 (->> to
                   (map (comp second class-name->garden str))
                   (filter map?)
                   (reduce merge)))})))

(def custom
  (into
    [{:id :max-w-prose
      :rules "
      max-w-prose = <'max-w-prose'>
      "
      :garden (fn [_]
                {:max-width "65ch"})}]
    apply-components))

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
     custom]))

(def my-color-map
  (assoc color/default-color-map
    "dark" "343a40"))

(def class-name->garden
  (:class-name->garden
    (make-api components
      {:color-map my-color-map
       :font-family-map typography/default-font-family-map})))
