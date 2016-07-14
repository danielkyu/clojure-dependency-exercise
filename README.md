# clojure-dependency-exercise
Solution to loading data in dependency order using Clojure

This is mostly meant to be run in the REPL as opposed to being run from the
command-line.

The main source file is located in src/app/core.clj while the test cases that
validate the logic are located in test/app/test/core.clj.

The dependency loading function can be executed via a call as such:

```clojure
(def sample-sources
  [{:name "sourceA"
    :dependencies #{"sourceB" "sourceC", "sourceD"}
    :data_source (fn[] (println "Fetching data for SourceA"))}
   
   {:name "sourceB"
    :dependencies #{"sourceE" "sourceF"}
    :data_source (fn[] (println "Fetching data for SourceB"))}
   
   {:name "sourceC"
    :dependencies #{"sourceD" "sourceF"}
    :data_source (fn[] (println "Fetching data for SourceC"))}
   
   {:name "sourceD"
    :dependencies #{}
    :data_source (fn[] (println "Fetching data for SourceD"))}
   
   {:name "sourceE"
    :dependencies #{}
    :data_source (fn[] (println "Fetching data for SourceE"))}
   
   {:name "sourceF"
    :dependencies #{}
    :data_source (fn[] (println "Fetching data for SourceF"))}])

(load-sources! sample-sources)

;; Prints the following
Fetching data for SourceE
Fetching data for SourceD
Fetching data for SourceF
Fetching data for SourceB
Fetching data for SourceC
Fetching data for SourceA
```
