;; Owner; yu.danielk@gmail.com
;;
;; Logic for loading sources in dependency order.

(ns app.core
  (:gen-class)
  (:require [clojure.set :as set]))

;; Sample test data that was provided as part of the question.
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

;; == Helper functions == 
;; Normally these would be moved to an internal namespace but it's placed here for
;; convenience/readability purposes.

(defn ->source-map
  "Given a collection of sources, constructs a Clojure map that associates the source name
   with the source itself."
  [sources]
  (->> sources
       (reduce (fn [source-map source]
                 (assoc source-map (:name source) source))
               {})))

(defn is-dependencies-loaded?
  "Given a set of source names that have already been loaded and a source to perform
   dependency checking, returns true if all the dependencies of the source have been loaded;
   false otherwise."
  [loaded source]
  (-> source
      (:dependencies)
      (set/subset? loaded)))

(defn find-loadable-sources
  "Given a source map and a set of sources names that correspond to sources that have
   already been loaded, returns a collection of sources that are able to be loaded."
  [pending loaded]
  (filter #(is-dependencies-loaded? loaded (second %)) pending))

(defn update-source-list
  "Given a source map that contains pending sources and an optional list of loaded sources,
   returns an updated list of loaded sources that has appended to it, new sources from the
   pending source map that have their dependencies loaded."
  ([pending]
   (update-source-list pending []))
  ([pending loaded]
   (let [loadable (set (find-loadable-sources pending (-> loaded keys set)))
         pending (remove #(loadable %) pending)
         loaded (concat loaded loadable)]
     ;; We break early if at least one of the two following conditions are met:
     ;;  1. All sources have been loaded.
     ;;  2. No loadable sources were found during the last load process.
     ;;     Since no change occurred, this means that there are sources that cannot be loaded
     ;;     as their dependencies were not present in the original source list.  As such, we
     ;;     break early and ignore these sources to prevent a stack overflow from infinite
     ;;     recursive calls.
     (if (or (empty? pending)
             (zero? (count loadable)))
       loaded
       (update-source-list pending loaded)))))

(defn get-source-list
  "Given a list of sources, returns a Clojure map that contains the following keys:
     :failed-sources - A set of sources that could not be loaded because of undefined
                       dependencies in the source list.
     :loadable-sources - A collection of sources in dependency order."
  [sources]
  (let [sources (->source-map sources)
        load (update-source-list sources)]
    {:failed-sources (set/difference (-> sources keys set) (-> load keys set))
     :loadable-sources load}))

;; == Interface function ==
;; Essentially the only function we'd expose to draw attention away from the underlying
;; implementation helper functions.

(defn load-sources!
  "Given a collection of sources, loads the sources in dependency order by invoking the
   function mapped to the `:data_source` key.  Sources that do not have any dependencies
   with respect to each other may be loaded in any order.  If two sources are defined with
   the same name, the latter will override the former.  The following keys must be defined
   for all sources:
     :name - The name of the source.
     :dependencies - A set of source names that correspond to sources that must be loaded
                     before this source can be loaded.
     :data_source - A function that will be executed when the source is loaded."
  [sources]
  (let [{:keys [failed-sources loadable-sources]} (get-source-list sources)]
    ;; Print out a list of sources that cannot be loaded (if there are any).
    ;; This is due to the fact that some sources are "unreachable" (they have dependencies
    ;; that are not present in the source list).
    (when (seq failed-sources)
      (println "Failed to load sources: " failed-sources))
    ;; Actually run the data source function to load the source.
    ;; Assume these functions may have side-effects.
    (doseq [source loadable-sources]
      ((-> source second :data_source)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (load-sources! sample-sources))
