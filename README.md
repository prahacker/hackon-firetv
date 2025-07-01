# Fire TV Recommendation System – Frontend

This is the frontend module of the **Fire TV Recommendation System**, designed for Android TV using the Leanback library. It presents a personalized, dynamic, and immersive interface for users to discover movies and shows, with deep linking support to OTT platforms like Netflix and Prime Video.

## Features

- **Dynamic Banners** – Show trailers or backdrops for hovered titles.
- **Auto-Refresh Recommendations** – Periodic updates from backend inference API.
- **Deep Linking** – Launch content directly in external OTT apps.
- **Behavior-Driven UI** – Recommendations based on user interaction logs (hover, click, scroll).
- **Social Rooms (Beta)** – Create or join social watch rooms via unique codes.

## Tech Stack

- **Kotlin**
- **Leanback Library**
- **AndroidX**
- **Glide** (for image loading)
- **WebView** (for trailers)
- **Firebase** (logging user behavior)

## Project Structure (Frontend Only)

app/
├── src/
│ └── main/
│ ├── java/com/example/firetv/
│ │ ├── MainActivity.kt # Entry point of the app
│ │ ├── MovieFragment.kt # Handles movie listing, banner, and interactions
│ │ ├── BannerFragment.kt # Displays horizontal banners and featured content
│ │ ├── CardPresenter.kt # Custom card UI for each movie
│ │ ├── Movie.kt # Data class representing a movie
│ │ ├── FirebaseLogger.kt # Logs hover/click/trailer data to Firebase
│ │ └── ... # Other fragments, utilities
│ ├── res/
│ │ ├── layout/ # XML UI layout files
│ │ ├── drawable/ # Icons, images, shapes
│ │ └── values/ # strings.xml, styles.xml, colors.xml, etc.
├── build.gradle # Gradle configuration for the app module
└── AndroidManifest.xml # Android app metadata and entry declarations
