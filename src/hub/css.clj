(ns hub.css)

(def apply-classes
  '{btn ["text-center" "py-2" "px-4" "bg-dark" "text-white"
         "rounded" "disabled:opacity-50" "hover:bg-black"]
    input-text ["border" "border-gray-400" "rounded" "w-full" "py-2" "px-3" "leading-tight"
                "appearance-none" "focus:outline-none" "focus:ring" "focus:border-blue-300"
                "ring-blue-300" "text-black" "ring-opacity-30"]
    link ["text-blue-600" "hover:underline"]})

(def components
  [{:id :max-w-prose
    :rules "
    max-w-prose = <'max-w-prose'>
           "
    :garden (fn [_]
              {:max-width "65ch"})}])

(def color-map
  {"dark" "343a40"})
