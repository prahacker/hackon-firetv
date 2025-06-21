package com.example.firetv

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.*
import android.widget.*
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

class MovieFragment : BrowseSupportFragment() {

    // UI Components
    private var bannerImageView: ImageView? = null
    private var bannerWebView: WebView? = null
    private var bannerOverlay: LinearLayout? = null
    private var bannerTitle: TextView? = null
    private var bannerDescription: TextView? = null
    private var unmuteButton: ImageView? = null
    private var bannerContainer: FrameLayout? = null
    private var bannerRatingBar: RatingBar? = null

    // State Variables
    private var focusedMovie: Movie? = null
    private var hoverStartTime = 0L
    private var lastHoveredMovieTitle: String? = null
    private var trailerStartTime: Long = 0L
    private var currentTrailerId: String? = null
    private var isMuted = true

    // Handlers and Runnables
    private var trailerHandler = Handler(Looper.getMainLooper())
    private var trailerRunnable: Runnable? = null
    private var trailerLoadTimeout: Runnable? = null

    // Adapters
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var recommendationAdapter: ArrayObjectAdapter

    // Constants
    private companion object {
        const val BANNER_HEIGHT_IMAGE = 0.32
        const val BANNER_HEIGHT_TRAILER = 0.78
        const val TRAILER_TIMEOUT = 8000L
        const val TRAILER_DELAY = 7000L
    }
    data class ShimmerCard(val id: Int = 0)

    data class CreateSocialRoomAction(
        val title: String = "Create Social Room",
        val description: String = "Start a new community discussion"
    )

    data class JoinSocialRoomAction(
        val title: String = "Join Social Room",
        val description: String = "Join existing community discussions"
    )
    // Data Classes

    // Lifecycle Methods
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            title = "Welcome to FireTV"
            initializeViews()
            initializeBannerSize()
            setupRowsAdapter()
            setupEventListeners()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in onActivityCreated: ${e.message}", e)
            title = "FireTV - Error Loading Content"
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            setupKeyListener()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in onResume: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        try {
            trailerHandler.removeCallbacksAndMessages(null)
            bannerWebView?.destroy()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in onDestroy: ${e.message}", e)
        }
    }

    // Initialization Methods
    private fun initializeViews() {
        try {
            bannerImageView = requireActivity().findViewById(R.id.banner)
            bannerWebView = requireActivity().findViewById(R.id.bannerWebView)
            bannerOverlay = requireActivity().findViewById(R.id.bannerOverlay)
            bannerTitle = requireActivity().findViewById(R.id.bannerTitle)
            bannerDescription = requireActivity().findViewById(R.id.bannerDescription)
            bannerContainer = requireActivity().findViewById(R.id.bannerContainer)
            bannerRatingBar = requireActivity().findViewById(R.id.bannerRatingBar)

            setupWebView()
            unmuteButton?.setOnClickListener { unmuteTrailer() }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error initializing views: ${e.message}", e)
        }
    }

    private fun setupWebView() {
        bannerWebView?.apply {
            clearCache(true)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("WebView_JS_Console",
                        "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("MovieFragment", "WebView loaded: $url")
                    trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
                }
            }
        }
    }

    private fun initializeBannerSize() {
        try {
            bannerContainer?.let { container ->
                val screenHeight = resources.displayMetrics.heightPixels
                val initialHeight = (screenHeight * BANNER_HEIGHT_IMAGE).toInt()
                container.layoutParams.height = initialHeight
                container.requestLayout()
                Log.d("MovieFragment", "Initial banner size set to: $initialHeight px")
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error setting initial banner size: ${e.message}", e)
        }
    }

    private fun setupKeyListener() {
        requireActivity().window.decorView.apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.action == KeyEvent.ACTION_DOWN) {
                    if (bannerWebView?.visibility == View.VISIBLE && isMuted && currentTrailerId != null) {
                        unmuteTrailer()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    // Adapter Setup Methods
    private fun setupRowsAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        setupPopularSection()
        setupRecommendationsSection()
        setupSocialRoomSection()
        adapter = rowsAdapter
    }

    private fun setupPopularSection() {
        val cardPresenter = CardPresenter()
        val movieList = MovieList.getMovies(requireContext())
        val rowAdapter = ArrayObjectAdapter(cardPresenter)

        movieList?.forEachIndexed { idx, movie ->
            if (isValidMovie(movie, idx)) {
                rowAdapter.add(movie)
            }
        }

        rowsAdapter.add(ListRow(HeaderItem(0, "Popular"), rowAdapter))
    }

    private fun setupRecommendationsSection() {
        recommendationAdapter = ArrayObjectAdapter(CardPresenter())
        rowsAdapter.add(ListRow(HeaderItem(1, "Recommendations"), recommendationAdapter))
        showShimmerSkeletons()

        val url = "http://10.0.2.2:8000/api/recommendation"
        RecommendationFetcher.fetchRecommendationsFromApi(url) { recs ->
            Handler(Looper.getMainLooper()).post {
                hideShimmerSkeletons()
                if (recs.isEmpty()) {
                    showRecSkeletons()
                } else {
                    recommendationAdapter.addAll(0, recs)
                }
            }
        }
    }

    private fun showShimmerSkeletons() {
        // Create a separate adapter for shimmer cards
        val shimmerAdapter = ArrayObjectAdapter(ShimmerPresenter())
        repeat(6) {
            shimmerAdapter.add(ShimmerCard(it))
        }

        // Replace the recommendation row with shimmer row
        val headerItem = HeaderItem(1, "Recommendations")
        val shimmerRow = ListRow(headerItem, shimmerAdapter)

        // Find and replace the recommendations row
        for (i in 0 until rowsAdapter.size()) {
            val row = rowsAdapter.get(i) as? ListRow
            if (row?.headerItem?.id == 1L) {
                rowsAdapter.replace(i, shimmerRow)
                break
            }
        }
    }

    private fun hideShimmerSkeletons() {
        // Replace shimmer row back with actual recommendation adapter
        val headerItem = HeaderItem(1, "Recommendations")
        val recommendationRow = ListRow(headerItem, recommendationAdapter)

        // Find and replace the shimmer row
        for (i in 0 until rowsAdapter.size()) {
            val row = rowsAdapter.get(i) as? ListRow
            if (row?.headerItem?.id == 1L) {
                rowsAdapter.replace(i, recommendationRow)
                break
            }
        }
    }

    private fun showRecSkeletons() {
        recommendationAdapter.clear()
        repeat(6) {
            recommendationAdapter.add(SkeletonMovie())
        }
    }


    private fun setupSocialRoomSection() {
        val socialRoomAdapter = ArrayObjectAdapter(ActionPresenter())
        socialRoomAdapter.add(CreateSocialRoomAction())
        socialRoomAdapter.add(JoinSocialRoomAction())
        rowsAdapter.add(ListRow(HeaderItem(2, "Community"), socialRoomAdapter))
    }



    // Event Listeners
    private fun setupEventListeners() {
        try {
            setOnItemViewSelectedListener { _, item, _, _ ->
                try {
                    when (item) {
                        is Movie -> handleMovieSelection(item)
                        is Recommendation -> handleRecommendationSelection(item)
                        is CreateSocialRoomAction -> handleCreateSocialRoomSelection(item)
                        is JoinSocialRoomAction -> handleJoinSocialRoomSelection(item)
                        else -> handleHeaderSelection()
                    }
                } catch (e: Exception) {
                    Log.e("MovieFragment", "Error in item selection: ${e.message}", e)
                }
            }

            setOnItemViewClickedListener { _, item, _, _ ->
                try {
                    when (item) {
                        is Movie -> handleMovieClick(item)
                        is Recommendation -> handleRecommendationClick(item)
                        is CreateSocialRoomAction -> handleCreateSocialRoomClick(item)
                        is JoinSocialRoomAction -> handleJoinSocialRoomClick(item)
                    }
                } catch (e: Exception) {
                    Log.e("MovieFragment", "Error in item click: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error setting up event listeners: ${e.message}", e)
        }
    }

    // Selection Handlers
    private fun handleMovieSelection(movie: Movie) {
        try {
            cleanupPreviousTrailer()
            logPreviousTrailerDuration()

            focusedMovie = movie
            lastHoveredMovieTitle = movie.title
            hoverStartTime = System.currentTimeMillis()

            FirebaseLogger.logHover(movie)
            updateBannerContent(movie)
            loadBackdropImage(movie)
            scheduleTrailerPlayback(movie)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling movie selection: ${e.message}", e)
        }
    }

    private fun handleRecommendationSelection(rec: Recommendation) {
        try {
            cleanupPreviousTrailer()
            focusedMovie = null
            currentTrailerId = null

            bannerTitle?.text = rec.title ?: "Unknown Title"
            bannerDescription?.text = rec.description ?: "No description available"

            val rating = ((rec.rating ?: 0f) / 2.0).coerceIn(0.0, 5.0)
            bannerRatingBar?.rating = rating.toFloat()
            bannerRatingBar?.visibility = View.VISIBLE

            val imageUrl = rec.backdrop_image ?: rec.poster_image
            loadImage(imageUrl)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling recommendation selection: ${e.message}", e)
        }
    }
    private fun handleCreateSocialRoomSelection(createRoom: CreateSocialRoomAction) {
        try {
            cleanupPreviousTrailer()
            focusedMovie = null
            currentTrailerId = null

            bannerTitle?.text = createRoom.title
            bannerDescription?.text = createRoom.description
            bannerRatingBar?.visibility = View.GONE

            bannerImageView?.let { imageView ->
                // Use Glide to properly load and scale the image
                Glide.with(requireContext())
                    .load(R.drawable.ic_social_room)
                    .centerCrop()
                    .into(imageView)
                fadeIn(imageView)
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling create social room selection: ${e.message}", e)
        }
    }

    private fun handleJoinSocialRoomSelection(joinRoom: JoinSocialRoomAction) {
        try {
            cleanupPreviousTrailer()
            focusedMovie = null
            currentTrailerId = null

            bannerTitle?.text = joinRoom.title
            bannerDescription?.text = joinRoom.description
            bannerRatingBar?.visibility = View.GONE

            bannerImageView?.let { imageView ->
                // Use Glide to properly load and scale the image
                Glide.with(requireContext())
                    .load(R.drawable.ic_social_room)
                    .centerCrop()
                    .into(imageView)
                fadeIn(imageView)
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling join social room selection: ${e.message}", e)
        }
    }


    private fun handleHeaderSelection() {
        focusedMovie = null
        currentTrailerId = null
        trailerHandler.removeCallbacksAndMessages(null)
        resetToImageMode()
        bannerTitle?.text = ""
        bannerDescription?.text = ""
        bannerRatingBar?.visibility = View.GONE
    }

    // Click Handlers
    private fun handleMovieClick(movie: Movie) {
        try {
            resetToImageMode()
            FirebaseLogger.logClick(movie)

            if (movie.deeplinks.size > 1) {
                showDeeplinkDialog(movie)
            } else {
                val homepage = movie.homepage
                if (!homepage.isNullOrBlank() && homepage != "N/A") {
                    DeepLinkHelper.launchUrl(requireContext(), homepage)
                } else {
                    Log.w("MovieFragment", "No valid homepage for movie: ${movie.title}")
                }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling movie click: ${e.message}", e)
        }
    }

    private fun handleRecommendationClick(rec: Recommendation) {
        resetToImageMode()
        val homepage = rec.homepage

        if (!homepage.isNullOrBlank() && homepage != "N/A") {
            DeepLinkHelper.launchUrl(requireContext(), homepage)
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Available On")
                .setItems(rec.available_platforms?.toTypedArray() ?: arrayOf("N/A"), null)
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun handleCreateSocialRoomClick(createRoom: CreateSocialRoomAction) {
        try {
            resetToImageMode()
            showCreateSocialRoomDialog()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling create social room click: ${e.message}", e)
        }
    }

    private fun handleJoinSocialRoomClick(joinRoom: JoinSocialRoomAction) {
        try {
            resetToImageMode()
            showJoinSocialRoomDialog()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling join social room click: ${e.message}", e)
        }
    }

    // Trailer Management
    private fun scheduleTrailerPlayback(movie: Movie) {
        try {
            if (focusedMovie != movie) {
                Log.d("MovieFragment", "Movie focus changed before scheduling, aborting")
                return
            }

            movie.trailer?.takeIf { it.contains("youtube.com/watch") }?.let { url ->
                val trailerId = url.substringAfter("v=").substringBefore("&")
                currentTrailerId = trailerId

                trailerRunnable = Runnable {
                    if (focusedMovie == movie && currentTrailerId == trailerId) {
                        Log.d("MovieFragment", "Validation PASSED. Playing trailer for ${movie.title}")
                        try {
                            playTrailer(trailerId, mute = true)
                        } catch (e: Exception) {
                            Log.e("MovieFragment", "Error playing trailer: ${e.message}", e)
                            fallbackToImage()
                        }
                    } else {
                        val currentFocus = focusedMovie?.title ?: "something else"
                        Log.d("MovieFragment", "Validation FAILED. Trailer for ${movie.title} aborted because focus is now on $currentFocus.")
                    }
                }

                trailerHandler.postDelayed(trailerRunnable!!, TRAILER_DELAY)
                Log.d("MovieFragment", "Trailer for ${movie.title} is scheduled to run in ${TRAILER_DELAY/1000} seconds.")
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error scheduling trailer: ${e.message}", e)
        }
    }

    private fun playTrailer(trailerId: String, mute: Boolean) {
        try {
            Log.d("MovieFragment", "Playing trailer: $trailerId")
            trailerStartTime = System.currentTimeMillis()

            trailerLoadTimeout = Runnable {
                Log.w("MovieFragment", "Trailer loading timeout, falling back to image")
                fallbackToImage()
            }
            trailerHandler.postDelayed(trailerLoadTimeout!!, TRAILER_TIMEOUT)

            resizeBanner(BANNER_HEIGHT_TRAILER)
            bannerOverlay?.visibility = View.GONE
            bannerImageView?.let { fadeOut(it) }

            Handler(Looper.getMainLooper()).postDelayed({
                bannerWebView?.visibility = View.VISIBLE
                loadTrailerContent(trailerId, mute)
            }, 200)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error playing trailer: ${e.message}", e)
            fallbackToImage()
        }
    }

    private fun loadTrailerContent(trailerId: String, mute: Boolean) {
        try {
            val muteParam = if (mute) 1 else 0
            val embedHtml = createTrailerEmbedHtml(trailerId, muteParam)

            Log.d("MovieFragment", "Loading enhanced trailer HTML for: $trailerId")
            bannerWebView?.loadDataWithBaseURL(
                "https://www.youtube.com",
                embedHtml,
                "text/html",
                "UTF-8",
                null
            )
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error loading trailer content: ${e.message}", e)
            fallbackToImage()
        }
    }

    private fun createTrailerEmbedHtml(trailerId: String, muteParam: Int): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { 
                        width: 100%; 
                        height: 100%; 
                        background: #000; 
                        overflow: hidden;
                        font-family: Arial, sans-serif;
                    }
                    .video-container {
                        position: relative;
                        width: 100%;
                        height: 100vh;
                        background: #000;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: none;
                        background: #000;
                    }
                    .loading {
                        position: absolute;
                        top: 50%;
                        left: 50%;
                        transform: translate(-50%, -50%);
                        color: white;
                        font-size: 18px;
                    }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <div class="loading" id="loading">Loading trailer...</div>
                    <iframe 
                        id="youtube-player"
                        src="https://www.youtube.com/embed/$trailerId?autoplay=1&mute=$muteParam&enablejsapi=1"
                        allow="autoplay; encrypted-media; picture-in-picture"
                        allowfullscreen
                    ></iframe>
                </div>
                <div id="fallback" style="display:none; text-align:center; padding-top:20px;">
                    <a href="https://www.youtube.com/watch?v=$trailerId" target="_blank" style="color:white;">
                        Watch on YouTube
                    </a>
                </div>
                <script>
                    console.log('Loading YouTube trailer: $trailerId');
                    var iframe = document.getElementById('youtube-player');
                    var loading = document.getElementById('loading');
                    iframe.onload = function() {
                        console.log('YouTube iframe loaded successfully');
                        loading.style.display = 'none';
                    };
                    iframe.onerror = function() {
                        console.log('YouTube iframe failed to load');
                        loading.innerHTML = 'Failed to load trailer';
                    };
                    setTimeout(function() {
                        if (loading.style.display !== 'none') {
                            console.log('YouTube loading timeout');
                            loading.innerHTML = 'Trailer unavailable';
                        }
                    }, 10000);
                    setTimeout(function() {
                        try {
                            iframe.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                        } catch(e) {
                            console.log('Could not send play command');
                        }
                    }, 3000);
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun unmuteTrailer() {
        try {
            if (currentTrailerId == null) return
            Log.d("MovieFragment", "Unmuting trailer: $currentTrailerId")
            isMuted = false
            unmuteButton?.visibility = View.GONE
            loadTrailerContent(currentTrailerId!!, false)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error unmuting trailer: ${e.message}", e)
        }
    }

    // UI Management
    private fun resetToImageMode() {
        try {
            destroyWebView()
            recreateWebView()

            bannerImageView?.visibility = View.VISIBLE
            bannerOverlay?.visibility = View.VISIBLE
            unmuteButton?.visibility = View.GONE
            resizeBanner(BANNER_HEIGHT_IMAGE)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error resetting to image mode: ${e.message}", e)
        }
    }

    private fun destroyWebView() {
        bannerWebView?.let { webView ->
            (webView.parent as? FrameLayout)?.removeView(webView)
            webView.destroy()
        }
        bannerWebView = null
    }

    private fun recreateWebView() {
        bannerWebView = WebView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setupWebViewSettings(this)
            visibility = View.GONE
        }
        bannerContainer?.addView(bannerWebView)
    }

    private fun setupWebViewSettings(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebView_JS_Console",
                    "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MovieFragment", "WebView loaded: $url")
                trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
            }
        }
    }

    private fun updateBannerContent(movie: Movie) {
        try {
            bannerTitle?.text = movie.title ?: "Unknown Title"
            bannerDescription?.text = movie.description ?: "No description available"

            val rating = ((movie.rating ?: 0.0) / 2.0).coerceIn(0.0, 5.0)
            bannerRatingBar?.rating = rating.toFloat()
            bannerRatingBar?.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error updating banner content: ${e.message}", e)
        }
    }

    private fun loadBackdropImage(movie: Movie) {
        loadImage(movie.backdrop_image)
    }

    private fun loadImage(imageUrl: String?) {
        try {
            bannerImageView?.let { imageView ->
                if (!imageUrl.isNullOrBlank() && imageUrl != "N/A") {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(imageView)
                    fadeIn(imageView)
                }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error loading image: ${e.message}", e)
        }
    }

    private fun resizeBanner(percent: Double) {
        try {
            bannerContainer?.let { container ->
                val screenHeight = resources.displayMetrics.heightPixels
                val newHeight = (screenHeight * percent).toInt()
                Log.d("MovieFragment", "Resizing banner to ${(percent * 100).toInt()}% ($newHeight px)")

                val currentHeight = container.layoutParams.height
                if (currentHeight != newHeight) {
                    animateResize(container, currentHeight, newHeight)
                } else {
                    container.layoutParams.height = newHeight
                    container.requestLayout()
                }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error resizing banner: ${e.message}", e)
        }
    }

    private fun animateResize(container: FrameLayout, currentHeight: Int, newHeight: Int) {
        val animator = ValueAnimator.ofInt(currentHeight, newHeight)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            try {
                container.layoutParams.height = animation.animatedValue as Int
                container.requestLayout()
            } catch (e: Exception) {
                Log.e("MovieFragment", "Error during resize animation: ${e.message}", e)
            }
        }
        animator.start()
    }

    // Animation Methods
    private fun fadeIn(view: View) {
        try {
            view.visibility = View.VISIBLE
            view.startAnimation(AlphaAnimation(0f, 1f).apply {
                duration = 400
                fillAfter = true
            })
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in fadeIn animation: ${e.message}", e)
            view.visibility = View.VISIBLE
        }
    }

    private fun fadeOut(view: View) {
        try {
            view.startAnimation(AlphaAnimation(1f, 0f).apply {
                duration = 300
                fillAfter = true
            })
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in fadeOut animation: ${e.message}", e)
            view.visibility = View.GONE
        }
    }

    // Dialog Methods
    private fun showCreateSocialRoomDialog() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Create Social Room")
                .setMessage("Create a new social room")
                .setPositiveButton("Create") { _, _ ->
                    Log.d("MovieFragment", "User created social room")
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error showing create social room dialog: ${e.message}", e)
        }
    }

    private fun showJoinSocialRoomDialog() {
        try {
            val input = EditText(requireContext()).apply {
                hint = "Enter room code"
                setPadding(32, 16, 32, 16)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Join Social Room")
                .setMessage("Enter the room code:")
                .setView(input)
                .setPositiveButton("Join") { _, _ ->
                    val roomCode = input.text.toString()
                    Log.d("MovieFragment", "Joining room: $roomCode")
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error showing join social room dialog: ${e.message}", e)
        }
    }

    private fun showDeeplinkDialog(movie: Movie) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Watch on")
                .setItems(movie.deeplinks.toTypedArray()) { _, idx ->
                    try {
                        resetToImageMode()
                        val platform = movie.deeplinks.getOrNull(idx)
                        val url = platformToUrl(platform ?: "", movie.homepage)
                        launchPlatformIntent(url, platform)
                    } catch (e: Exception) {
                        Log.e("MovieFragment", "Error launching deeplink: ${e.message}", e)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error showing deeplink dialog: ${e.message}", e)
        }
    }

    // Utility Methods
    private fun isValidMovie(movie: Movie?, index: Int): Boolean {
        if (movie == null) {
            Log.w("MovieFragment", "Movie at index $index is null")
            return false
        }
        if (movie.title.isNullOrBlank()) {
            Log.w("MovieFragment", "Movie title is missing at index $index")
            return false
        }
        if (movie.backdrop_image.isNullOrBlank() || movie.backdrop_image == "N/A") {
            Log.w("MovieFragment", "Movie backdrop is missing or N/A at index $index (${movie.title})")
            return false
        }
        if (movie.description.isNullOrBlank()) {
            movie.description = "No description available"
        }
        return true
    }

    private fun cleanupPreviousTrailer() {
        trailerHandler.removeCallbacksAndMessages(null)
        trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
        trailerRunnable = null
        trailerLoadTimeout = null
        resetToImageMode()
    }

    private fun logPreviousTrailerDuration() {
        if (trailerStartTime != 0L && lastHoveredMovieTitle != null) {
            val playedDuration = System.currentTimeMillis() - trailerStartTime
            FirebaseLogger.logTrailerDuration(lastHoveredMovieTitle!!, playedDuration)
        }
        trailerStartTime = 0L
        currentTrailerId = null
    }

    private fun fallbackToImage() {
        try {
            Log.w("MovieFragment", "Falling back to image display")
            trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
            resetToImageMode()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in fallback: ${e.message}", e)
        }
    }

    private fun platformToUrl(platform: String, homepage: String?): String? {
        return when {
            platform.contains("Netflix", ignoreCase = true) -> homepage ?: "https://www.netflix.com/"
            platform.contains("Amazon Prime", ignoreCase = true) -> homepage ?: "https://www.primevideo.com/"
            platform.contains("YouTube", ignoreCase = true) -> homepage ?: "https://www.youtube.com/"
            else -> homepage
        }
    }

    private fun launchPlatformIntent(url: String?, platform: String?) {
        if (!url.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Set package for specific platforms
                when {
                    platform?.contains("Netflix", ignoreCase = true) == true ->
                        setPackage("com.netflix.mediaclient")
                    platform?.contains("Prime", ignoreCase = true) == true ->
                        setPackage("com.amazon.avod.thirdpartyclient")
                }
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to browser
                intent.setPackage(null)
                startActivity(intent)
            }
        }
    }
}
