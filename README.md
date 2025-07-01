# Fire TV App

This is a Fire TV application that showcases a movie Browse and viewing experience. It appears to be a sophisticated app with a rich user interface, dynamic content loading, and features that enhance user engagement.

## Features

* **Dynamic UI:** The app features a dynamic UI that adjusts card sizes based on user interaction. It includes a banner for showcasing featured content that can display images and play trailers.
* **Movie Browse:** Users can browse movies categorized by "Popular" and various genres like "Thriller," "Horror," and "Comedy".
* **Movie Details:** Selecting a movie opens a detailed view with a banner, description, and lists of similar and recommended movies.
* **Trailer Playback:** The app supports inline trailer playback within the banner.
* **Recommendations:** The app fetches and displays movie recommendations from an external API, with a shimmer effect shown while loading.
* **Voice Commands:** Voice command integration allows users to perform actions like "surprise me".
* **Platform Integration:** The app displays logos for different streaming platforms like Netflix and Prime Video and can launch them via deep links.

## Tech Stack

* **Android SDK:** The core of the application is built on the Android SDK.
* **Leanback Library:** Used for creating the TV-optimized user interface, including `BrowseSupportFragment`, `DetailsSupportFragment`, and various presenters.
* **Glide:** For image loading and transformations, including blur effects for backgrounds.
* **Shimmer (by Facebook):** Used to create loading animations.
* **Firebase:** For logging user interactions and analytics.
* **GSON:** For parsing JSON data from assets and APIs.
* **OkHttp:** Used for making network requests to the recommendation API.

## Setup

To run this project, you will need:

1.  Android Studio.
2.  An Android device or emulator running a compatible version of Android.
3.  The necessary SDKs and build tools installed.

Once the project is opened in Android Studio, it should be possible to build and run it on a connected device or emulator. The app uses a local JSON file for its movie data (`netflix_prime_content_deduped.json`), which is located in the `assets` folder. For the recommendation features to work, the backend service at `http://13.201.101.100` needs to be accessible.

## File Structure

The project follows a standard Android application structure. Here's a breakdown of the key directories and files:

* `java/com/example/firetv/`: This is the main package containing all the Kotlin source code.
    * `MainActivity.kt`: The main entry point of the application.
    * `MovieFragment.kt`: The primary fragment for Browse movies, which uses the Leanback library.
    * `DetailsActivity.kt` and `VideoDetailsFragment.kt`: Handle the movie details screen.
    * `presenter/`: Contains various presenters for rendering different UI components in the Leanback framework, such as `MoviePresenter` and `CardPresenter`.
    * `MovieList.kt`, `MovieListProvider.kt`, `RecommendationFetcher.kt`: These files are responsible for providing movie data to the application, either from local assets or a remote API.
* `res/`: Contains all the resources for the application.
    * `layout/`: XML layout files for activities, fragments, and list items.
    * `drawable/`: Vector drawables and other image assets.
    * `values/`: String, color, and theme definitions.
    * `font/`: Font files used in the application.
* `assets/`: Contains the `netflix_prime_content_deduped.json` file, which serves as the primary data source for the movie catalog.
* `AndroidManifest.xml`: The application manifest file, which declares the app's components and permissions.
