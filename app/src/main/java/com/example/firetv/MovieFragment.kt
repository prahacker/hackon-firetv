package com.example.firetv
import android.view.ViewTreeObserver
import androidx.leanback.widget.RowHeaderView
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.example.firetv.model.toMovie
import android.os.AsyncTask
import android.os.Bundle
import java.io.IOException
import okhttp3.OkHttpClient
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import okhttp3.Request
import org.json.JSONObject
//import com.example.firetv.Converters
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.*
import android.widget.*
import androidx.databinding.adapters.Converters
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.example.firetv.model.Recommendation
import com.example.firetv.model.SkeletonMovie
import java.net.URL


class   MovieFragment : BrowseSupportFragment() {
    private var isZoomedMode: Boolean = false

    private var lastLoggedItem: Any? = null
    private var lastLoggedAction: String? = null
    private var lastLogTime: Long = 0L
    private val LOG_DEBOUNCE_MS = 60L
    private lateinit var allMovies: List<Movie>
    private var spotlightBorder: SpotlightBorderView? = null
    private var bannerImageView: ImageView? = null
    private var bannerWebView: WebView? = null
    private var bannerOverlay: LinearLayout? = null
    private var lastTrailerProgress = 0
    private var bannerTitle: TextView? = null
    private var bannerDescription: TextView? = null
    private var unmuteButton: ImageView? = null
    private var currentHoverItem: Any? = null
    private var bannerContainer: FrameLayout? = null
    private var bannerRatingBar: RatingBar? = null
    private var recommendationPopup: View? = null
    private var popupContainer: ViewGroup? = null
    private val popupHandler = Handler(Looper.getMainLooper())
    private var popupDismissRunnable: Runnable? = null
    private lateinit var voiceHelper: VoiceCommandHelper

    // State Variables
    private var focusedMovie: Movie? = null

    private var lastHoveredMovieTitle: String? = null
    private var trailerStartTime: Long = 0L
    private var currentTrailerId: String? = null
    private var isMuted = true
    private var reasoningPopup: PopupWindow? = null
    private val reasoningHandler = Handler(Looper.getMainLooper())

    private var lastHoverStartTime: Long = System.currentTimeMillis()
    private var currentFocusedMovie: Movie? = null
    private var shufflePlayerContainer: FrameLayout? = null

    private val autoUpdateHandler = Handler(Looper.getMainLooper())
    private var autoUpdateRunnable: Runnable? = null
    private var trailerHandler = Handler(Looper.getMainLooper())
    private var trailerRunnable: Runnable? = null
    private var trailerLoadTimeout: Runnable? = null

    private var lastAnimatedView: View? = null
    // Adapters
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var recommendationAdapter: ArrayObjectAdapter

    // Constants
    private companion object {
        const val POPUP_DURATION = 4000L
        const val POPUP_FADE_DURATION = 300L
        const val BANNER_HEIGHT_IMAGE = 0.38
        const val BANNER_HEIGHT_SOCIAL = 0.55
        const val BANNER_HEIGHT_TRAILER = 0.78
        const val TRAILER_TIMEOUT = 8000L
        const val TRAILER_DELAY = 5000L
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shufflePlayerContainer = requireActivity().findViewById(R.id.shuffle_player_container)

        // ✨ NEW: remember the last D-pad key that was pressed
        view.isFocusableInTouchMode = true          // make root view eligible for key events
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        SequenceTracker.lastDpadKeyCode = keyCode
                    }
                }
            }
            false   // let Leanback handle normal focus navigation
        }

        initializeViews()
        setupEventListeners()
        setupRowsAdapter()
        startAutoRecommendationUpdater()
        val customHeaderPresenter = CustomRowHeaderPresenter()
        setHeaderPresenterSelector(object : androidx.leanback.widget.PresenterSelector() {
            override fun getPresenter(item: Any?): androidx.leanback.widget.Presenter {
                return customHeaderPresenter
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title = "Welcome to FireTV"
    }

    private fun handleKeyPress(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (recommendationPopup != null) {
                    dismissRecommendationPopup()
                    true
                } else handleCenterButtonPress()
            }
            else -> false
        }
    }

    private fun handleCenterButtonPress(): Boolean {
        return try {
            when {
                focusedMovie != null -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling center button: ${e.message}", e)
            false
        }
    }
    override fun onStart() {
        super.onStart()
        try {
            headersSupportFragment.view?.background = ColorDrawable(Color.parseColor("#1C1C1E"))
        } catch (e: Exception) {
            Log.e("MovieFragment", "Could not set headers background color", e)
        }
    }

    private fun showRecommendationPopup() {
        try {
            if (recommendationPopup != null || context == null) return
            val inflater = LayoutInflater.from(requireContext())
            recommendationPopup = inflater.inflate(R.layout.layout_recommendation_popup, popupContainer, false)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                topMargin = (resources.displayMetrics.heightPixels * 0.4).toInt()
                rightMargin = 32
            }
            recommendationPopup?.layoutParams = layoutParams
            recommendationPopup?.alpha = 0f
            popupContainer?.addView(recommendationPopup)
            recommendationPopup?.animate()
                ?.alpha(1f)
                ?.setDuration(POPUP_FADE_DURATION)
                ?.start()
            schedulePopupDismiss()
            Log.d("MovieFragment", "Recommendation popup displayed")
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error showing recommendation popup: ${e.message}", e)
        }
    }
    private fun safeLogInteraction(
        actionType: String,
        focusedItem: Any?,
        hoverDuration: Float? = null,
        timeSinceLastAction: Long? = null,
        consecutiveActionCount: Int? = null,
        clickType: String? = null,
        playbackPosition: Long? = null
    ) {
        val now = System.currentTimeMillis()
        // Only log if it's not a duplicate within LOG_DEBOUNCE_MS
        if (
            (focusedItem != lastLoggedItem || actionType != lastLoggedAction) ||
            (now - lastLogTime) > LOG_DEBOUNCE_MS
        ) {
            if (consecutiveActionCount != null) {
                if (timeSinceLastAction != null) {
                    FirebaseLogger.logInteraction(
                        actionType = actionType,
                        screenContext = "Home",
                        focusedItem = focusedItem,
                        hoverDuration = hoverDuration,
                        timeSinceLastAction = timeSinceLastAction.toFloat(),
                        consecutiveActionCount = consecutiveActionCount,
                        clickType = clickType,
                        playbackPosition = playbackPosition
                    )
                }
            }
            lastLoggedItem = focusedItem
            lastLoggedAction = actionType
            lastLogTime = now
        }
    }

    private fun dismissRecommendationPopup() {
        try {
            recommendationPopup?.let { popup ->
                popupDismissRunnable?.let { popupHandler.removeCallbacks(it) }
                popup.animate()
                    .alpha(0f)
                    .setDuration(POPUP_FADE_DURATION)
                    .withEndAction {
                        try {
                            popupContainer?.removeView(popup)
                            recommendationPopup = null
                        } catch (e: Exception) {
                            Log.e("MovieFragment", "Error removing popup: ${e.message}", e)
                        }
                    }
                    .start()
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error dismissing recommendation popup: ${e.message}", e)
        }
    }

    private fun schedulePopupDismiss() {
        popupDismissRunnable = Runnable { dismissRecommendationPopup() }
        popupHandler.postDelayed(popupDismissRunnable!!, POPUP_DURATION)
    }

    override fun onResume() {
        super.onResume()
        uiModePollHandler.post(uiModePollRunnable)
    }

    override fun onPause() {
        super.onPause()
        try {
            cleanupPreviousTrailer()
            logPreviousTrailerDuration()
            dismissRecommendationPopup()
            uiModePollHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in onPause: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoUpdateHandler.removeCallbacksAndMessages(null)
        trailerHandler.removeCallbacksAndMessages(null)
        popupHandler.removeCallbacksAndMessages(null)
        reasoningHandler.removeCallbacksAndMessages(null)
        reasoningPopup?.dismiss()
        bannerWebView?.let {
            it.stopLoading()
            it.onPause()
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        bannerWebView = null
        bannerImageView = null
        bannerContainer = null
        bannerOverlay = null
        bannerTitle = null
        bannerDescription = null
        bannerRatingBar = null
        unmuteButton = null
        popupContainer = null
    }

    private fun initializeViews() {
        try {
            val activityView = requireActivity().window.decorView
            bannerContainer = activityView.findViewById(R.id.bannerContainer)
            bannerImageView = activityView.findViewById(R.id.banner)
            bannerWebView = activityView.findViewById(R.id.bannerWebView)
            bannerOverlay = activityView.findViewById(R.id.bannerOverlay)
            bannerTitle = activityView.findViewById(R.id.bannerTitle)
            bannerDescription = activityView.findViewById(R.id.bannerDescription)
            bannerRatingBar = activityView.findViewById(R.id.bannerRatingBar)
            unmuteButton = activityView.findViewById(R.id.unmuteButton)
            popupContainer = activityView.findViewById(android.R.id.content)
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
            addJavascriptInterface(TrailerWebViewInterface(), "Android")
        }
    }

    private fun setupRowsAdapter() {
        val listRowPresenter = object : ListRowPresenter(FocusHighlight.ZOOM_FACTOR_LARGE) {}
        listRowPresenter.shadowEnabled = false
        rowsAdapter = ArrayObjectAdapter(listRowPresenter)
        setupPopularSection()
        setupRecommendationsSection()
        setupSocialRoomSection()
        adapter = rowsAdapter

        setSelectedPosition(0)
    }

    private fun setupPopularSection() {
        val cardPresenter = CardPresenter { isZoomedMode }
        MovieList.getMovies(requireContext()) { movieList ->
            allMovies = movieList
            val popularAdapter = ArrayObjectAdapter(cardPresenter)
            // The "Popular" row can remain ordered by rating or as is.
            movieList.take(15).forEachIndexed { idx, movie ->
                if (isValidMovie(movie, idx)) {
                    popularAdapter.add(movie)
                }
            }
            rowsAdapter.add(ListRow(HeaderItem(0, "Popular"), popularAdapter))

            val genresToShow = listOf("Thriller", "Horror", "Comedy", "Action", "Romance")
            var headerId = 1
            for (genre in genresToShow) {
                val genreCardPresenter = CardPresenter { isZoomedMode }
                val genreAdapter = ArrayObjectAdapter(genreCardPresenter)


                val filteredMovies = movieList
                    .filter { it.genres.contains(genre) }
                    .shuffled()
                    .take(20)


                filteredMovies.forEachIndexed { idx, movie ->
                    if (isValidMovie(movie, idx)) {
                        genreAdapter.add(movie)
                    }
                }
                if (genreAdapter.size() > 0) {
                    rowsAdapter.add(ListRow(HeaderItem(headerId++.toLong(), genre), genreAdapter))
                }
            }
        }
    }



    private fun scheduleRecommendationTrailer(rec: Recommendation) {
        try {
            val trailerUrl = rec.trailer
            if (trailerUrl?.contains("youtube.com/watch") == true) {
                val trailerId = trailerUrl.substringAfter("v=").substringBefore("&")
                currentTrailerId = trailerId
                trailerRunnable = Runnable {
                    if (currentTrailerId == trailerId) {
                        Log.d("MovieFragment", "Playing trailer for recommendation: ${rec.title}")
                        try {
                            playTrailer(trailerId, mute = true)
                        } catch (e: Exception) {
                            Log.e("MovieFragment", "Error playing recommendation trailer: ${e.message}", e)
                            fallbackToImage()
                        }
                    }
                }
                trailerHandler.postDelayed(trailerRunnable!!, TRAILER_DELAY)
                Log.d("MovieFragment", "Trailer for ${rec.title} scheduled")
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error scheduling recommendation trailer: ${e.message}", e)
        }
    }

    private fun showShimmerSkeletons() {
        val shimmerAdapter = ArrayObjectAdapter(ShimmerPresenter())
        repeat(6) {
            shimmerAdapter.add(ShimmerCard(it))
        }
        val headerItem = HeaderItem(1, "Recommendations")
        val shimmerRow = ListRow(headerItem, shimmerAdapter)
        for (i in 0 until rowsAdapter.size()) {
            val row = rowsAdapter.get(i) as? ListRow
            if (row?.headerItem?.id == 1L) {
                rowsAdapter.replace(i, shimmerRow)
                break
            }
        }
    }

    private fun setupRecommendationsSection() {
        recommendationAdapter = ArrayObjectAdapter(CardPresenter {isZoomedMode})
        // rowsAdapter.add(ListRow(HeaderItem(1, "Recommended For You"), recommendationAdapter))
        rowsAdapter.add(ListRow(HeaderItem(1, "Recommendations"), recommendationAdapter))
        showShimmerSkeletons()
        val decorateUrl = "http://3.111.57.57:8080/decorate"
        RecommendationFetcher.fetchRecommendationsFromApi(decorateUrl) { recs ->
            Handler(Looper.getMainLooper()).post {
                recommendationAdapter.clear()
                if (recs.isEmpty()) {
                    Log.w("MovieFragment", "⚠️ No recommendations found. Showing skeletons.")
                    showShimmerSkeletons()
                } else {
                    hideShimmerSkeletons()
                    recommendationAdapter.addAll(0, recs)
                }
            }
        }
    }

    private fun startAutoRecommendationUpdater() {
        autoUpdateRunnable = object : Runnable {
            override fun run() {
                val generateUrl = "http://3.111.57.57:5000/recommendation"
                val decorateUrl = "http://3.111.57.57:8080/decorate"
                AsyncTask.execute {
                    try {

                        URL(generateUrl).readText()
                        Log.d("MovieFragment", "Triggered recommendation generation")

                        Handler(Looper.getMainLooper()).postDelayed({
                            RecommendationFetcher.fetchRecommendationsFromApi(decorateUrl) { recs ->
                                Handler(Looper.getMainLooper()).post {
                                    recommendationAdapter.clear()
                                    if (recs.isEmpty()) {
                                        Log.w("MovieFragment", "Empty recommendations")
                                        showShimmerSkeletons()
                                    } else {
                                        hideShimmerSkeletons()
                                        recommendationAdapter.addAll(0, recs)
                                    }
                                }
                            }
                        }, 15_000)
                    } catch (e: Exception) {
                        Log.e("MovieFragment", "Error during recommendation generation: ${e.message}", e)
                    }
                }
                autoUpdateHandler.postDelayed(this, 20_000)
            }
        }
        autoUpdateHandler.post(autoUpdateRunnable!!)
    }





    private fun hideShimmerSkeletons() {
        val headerItem = HeaderItem(1, "Recommendations")
        val recommendationRow = ListRow(headerItem, recommendationAdapter)
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
    private fun isContentItem(item: Any?): Boolean {
        return item is Movie || item is Recommendation || item is CreateSocialRoomAction || item is JoinSocialRoomAction
    }
    private fun setupSocialRoomSection() {
        val socialRoomAdapter = ArrayObjectAdapter(ActionPresenter())
        socialRoomAdapter.add(CreateSocialRoomAction())
        socialRoomAdapter.add(JoinSocialRoomAction())
        rowsAdapter.add(ListRow(HeaderItem(2, "Community"), socialRoomAdapter))
    }

    private fun handleCreateSocialRoomSelection(item: CreateSocialRoomAction) {
        Log.d("MovieFragment", "CreateSocialRoomAction selected: $item")
    }

    private fun setupEventListeners() {
        try {
            setOnItemViewSelectedListener(object : OnItemViewSelectedListener {
                override fun onItemSelected(
                    itemViewHolder: Presenter.ViewHolder?,
                    item: Any?,
                    rowViewHolder: RowPresenter.ViewHolder?,
                    row: Row?
                ) {
                    // --- Animation Logic ---
                    // First, reset the previously animated view to its normal state
                    lastAnimatedView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.alpha(1.0f)?.setDuration(200)?.start()
                    lastAnimatedView = null

                    // Check if the selected item is in the "Recommendations" row (ID 1)
                    val isRecommendation = (row as? ListRow)?.headerItem?.id == 1L

                    if (itemViewHolder != null && isRecommendation) {
                        val targetView = itemViewHolder.view
                        targetView.animate()
                            .scaleX(1.15f)
                            .scaleY(1.15f)
                            .alpha(1.0f)
                            .setDuration(200)
                            .start()
                        lastAnimatedView = targetView
                    }
                    // --- End of Animation Logic ---


                    // --- Your Existing Logging and Focus Logic ---
                    val now = System.currentTimeMillis()
                    if (currentHoverItem != null && currentHoverItem != item) {
                        val dur = now - lastHoverStartTime
                        if (dur > 50) {
                            FirebaseLogger.logInteraction(
                                actionType            = "hover",
                                screenContext         = "Home",
                                focusedItem           = currentHoverItem,
                                hoverDuration         = dur / 1000f,
                                timeSinceLastAction   = SequenceTracker.timeSinceLastAction(),
                                consecutiveActionCount= SequenceTracker.increment("hover")
                            )
                        }
                    }
                    if (isContentItem(item)) {
                        currentHoverItem   = item
                        lastHoverStartTime = now
                        FocusTracker.setFocus(item)
                    }

                    SequenceTracker.lastDpadKeyCode?.let { code ->
                        val act = when (code) {
                            KeyEvent.KEYCODE_DPAD_UP    -> "dpad_up"
                            KeyEvent.KEYCODE_DPAD_DOWN  -> "dpad_down"
                            KeyEvent.KEYCODE_DPAD_LEFT  -> "dpad_left"
                            KeyEvent.KEYCODE_DPAD_RIGHT -> "dpad_right"
                            else -> null
                        }
                        act?.let {
                            FirebaseLogger.logInteraction(
                                actionType            = it,
                                screenContext         = "Home",
                                focusedItem           = FocusTracker.getLastFocusedItem(),
                                timeSinceLastAction   = SequenceTracker.timeSinceLastAction(),
                                consecutiveActionCount= SequenceTracker.increment(it)
                            )
                        }
                        SequenceTracker.lastDpadKeyCode = null
                    }

                    updateUIForSelectedItem(item)
                }
            })

            setOnItemViewClickedListener { _, item, _, _ ->
                FocusTracker.setFocus(item)
                handleItemClick(item)
            }

            requireView().viewTreeObserver.addOnGlobalFocusChangeListener(
                ViewTreeObserver.OnGlobalFocusChangeListener { oldF, newF ->
                    val oldIsHeader = oldF is RowHeaderView
                    val newIsHeader = newF is RowHeaderView

                    if (!oldIsHeader && newIsHeader) {
                        val now = System.currentTimeMillis()
                        currentHoverItem?.let {
                            val dur = now - lastHoverStartTime
                            if (dur > 50) {
                                FirebaseLogger.logInteraction(
                                    actionType            = "hover",
                                    screenContext         = "Home",
                                    focusedItem           = it,
                                    hoverDuration         = dur / 1000f,
                                    timeSinceLastAction   = SequenceTracker.timeSinceLastAction(),
                                    consecutiveActionCount= SequenceTracker.increment("hover")
                                )
                            }
                            currentHoverItem = null
                        }
                        SequenceTracker.lastDpadKeyCode?.takeIf { it == KeyEvent.KEYCODE_DPAD_LEFT }?.let {
                            FirebaseLogger.logInteraction(
                                actionType            = "dpad_left",
                                screenContext         = "Home",
                                focusedItem           = FocusTracker.getLastFocusedItem(),
                                timeSinceLastAction   = SequenceTracker.timeSinceLastAction(),
                                consecutiveActionCount= SequenceTracker.increment("dpad_left")
                            )
                            SequenceTracker.lastDpadKeyCode = null
                        }
                    }

                    if (oldIsHeader && !newIsHeader) {
                        SequenceTracker.lastDpadKeyCode?.takeIf { it == KeyEvent.KEYCODE_DPAD_RIGHT }?.let {
                            FirebaseLogger.logInteraction(
                                actionType            = "dpad_right",
                                screenContext         = "Home",
                                focusedItem           = FocusTracker.getLastFocusedItem(),
                                timeSinceLastAction   = SequenceTracker.timeSinceLastAction(),
                                consecutiveActionCount= SequenceTracker.increment("dpad_right")
                            )
                            SequenceTracker.lastDpadKeyCode = null
                        }
                    }
                })
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error setting up event listeners: ${e.message}", e)
        }

    }



    private fun launchDeeplink(movie: Movie) {
        stopTrailerPlayback()
        cleanupPreviousTrailer()
        val platform = movie.deeplinks.firstOrNull { it.contains("netflix", ignoreCase = true) }
        if (platform != null) {
            val url = platformToUrl(platform, movie.homepage)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            FirebaseLogger.logInteraction(
                actionType = "click",
                screenContext = "Detail_Page",
                focusedItem = movie,
                timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                consecutiveActionCount = SequenceTracker.increment("click"),
                clickType = "play"
            )
            startActivity(intent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(movie.homepage)
                setPackage("com.amazon.cloud9")
            }
            FirebaseLogger.logInteraction(
                actionType = "click",
                screenContext = "Detail_Page",
                focusedItem = movie,
                timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                consecutiveActionCount = SequenceTracker.increment("click"),
                clickType = "play"
            )
            startActivity(browserIntent)
        }
    }

    private fun handleMovieSelection(movie: Movie) {
        try {
            cleanupPreviousTrailer()
            logPreviousTrailerDuration()

            focusedMovie = movie
            lastHoveredMovieTitle = movie.title
            updateBannerContent(movie)
            loadBackdropImage(movie)
            scheduleTrailerPlayback(movie)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling movie selection: ${e.message}", e)
        }
    }

    private fun logTrailerCompleted() {
        try {
            FirebaseLogger.logInteraction(
                actionType = "playback_completed",
                screenContext = "Home",
                focusedItem = currentFocusedMovie,
                timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                consecutiveActionCount = SequenceTracker.increment("playback_completed"),
                playbackPosition = lastTrailerProgress.toLong()
            )
            lastTrailerProgress = 0
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error logging trailer completed: ${e.message}", e)
        }
    }

    inner class TrailerWebViewInterface {
        @JavascriptInterface
        fun onTrailerEnd() {
            activity?.runOnUiThread {
                logTrailerCompleted()
            }
        }

        @JavascriptInterface
        fun onTrailerProgress(percent: Int) {
            activity?.runOnUiThread {
                lastTrailerProgress = percent
                Log.d("MovieFragment", "Trailer progress updated: $percent%")
            }
        }
    }

    private fun handleJoinSocialRoomSelection(joinRoom: JoinSocialRoomAction) {
        try {
            cleanupPreviousTrailer()
            resizeBanner(BANNER_HEIGHT_SOCIAL)
            focusedMovie = null
            currentTrailerId = null
            bannerTitle?.text = joinRoom.title
            bannerDescription?.text = joinRoom.description
            bannerRatingBar?.visibility = View.GONE
            bannerImageView?.let { imageView ->
                context?.let {
                    Glide.with(it)
                        .load(R.drawable.ic_social_room)
                        .centerCrop()
                        .into(imageView)
                    fadeIn(imageView)
                }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling join social room selection: ${e.message}", e)
        }
    }

    private fun handleHeaderSelection() {
        focusedMovie = null
        currentTrailerId = null
        trailerHandler.removeCallbacksAndMessages(null)
        stopTrailerAndResetBanner()
        bannerTitle?.text = ""
        bannerDescription?.text = ""
        bannerRatingBar?.visibility = View.GONE
    }

    private fun stopTrailerAndResetBanner() {
        try {
            bannerWebView?.let { webView ->
                webView.evaluateJavascript("if(typeof player !== 'undefined' && player.pauseVideo) { player.pauseVideo(); }", null)
                webView.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    webView.loadUrl("about:blank")
                }, 100)
            }
            bannerImageView?.visibility = View.VISIBLE
            bannerOverlay?.visibility = View.VISIBLE
            unmuteButton?.visibility = View.GONE
            resizeBanner(BANNER_HEIGHT_IMAGE)
            currentTrailerId = null
            isMuted = true
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in stopTrailerAndResetBanner: ${e.message}", e)
        }
    }
    fun surpriseMe() {
        val count = recommendationAdapter.size()
        if (count == 0) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Recommendations")
                .setMessage("There are no recommendations available at the moment. Please try again later.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val idx = (0 until count).random()
        val movie = recommendationAdapter.get(idx) as? Movie ?: return

        // Fetch reasoning FIRST, then show the dialog with the reasoning.
        RecommendationFetcher.fetchReasoningForTitle(movie.title) { reasoning ->
            activity?.runOnUiThread {
                val recommendedMovies = (0 until recommendationAdapter.size())
                    .mapNotNull { recommendationAdapter.get(it) as? Movie }
                    .filter { it != movie }
                val dialog = MovieDetailsDialogFragment(movie, allMovies, recommendedMovies, reasoning)
                    .setDeeplinkListener(object : MovieDetailsDialogFragment.DeeplinkListener {
                        override fun onPlayClicked(movie: Movie) {
                            launchDeeplink(movie)
                        }
                    })

                dialog.onDialogDismiss = {
                    focusedMovie?.let { handleMovieSelection(it) }
                }
                dialog.show(parentFragmentManager, "MovieDetailsDialog")
            }
        }
    }


    fun showBanner(message: String) {
        val banner = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        banner.show()
    }

    fun stopTrailerPlayback() {
        try {
            bannerWebView?.let { webView ->
                if (webView.visibility == View.VISIBLE) {
                    webView.evaluateJavascript("if(typeof player !== 'undefined' && player.pauseVideo) { player.pauseVideo(); }", null)
                    Handler(Looper.getMainLooper()).postDelayed({
                        webView.loadUrl("about:blank")
                        webView.visibility = View.GONE
                        Log.d("MovieFragment", "Stopped trailer playback")
                    }, 100)
                }
            }
            bannerImageView?.visibility = View.VISIBLE
            bannerOverlay?.visibility = View.VISIBLE
            unmuteButton?.visibility = View.GONE
            currentTrailerId = null
            isMuted = true
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error stopping trailer playback: ${e.message}", e)
        }
    }
    private val httpClient = OkHttpClient()
    private fun fetchZoomValueAndUpdateUI() {
        val apiUrl = "http://3.111.57.57:8080/cognitive_load"
        val request = Request.Builder().url(apiUrl).build()

        // Use a background thread for networking
        Thread {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("MovieFragment", "API call failed with code: ${response.code}")
                        return@Thread
                    }

                    val result = response.body?.string()
                    if (result == null) {
                        Log.e("MovieFragment", "API response body is null")
                        return@Thread
                    }

                    Log.d("MovieFragment", "Zoom API Response: $result")

                    // The rest of your parsing and UI update logic
                    val value = JSONObject(result).getDouble("value")
                    val newZoomedMode = value > 0.5

                    if (isAdded && activity != null) {
                        if (newZoomedMode != isZoomedMode) {
                            isZoomedMode = newZoomedMode
                            requireActivity().runOnUiThread {
                                updateCardSizes()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("MovieFragment", "Network request failed for zoom value: ${e.message}")
            } catch (e: Exception) {
                Log.e("MovieFragment", "Error processing zoom value: ${e.message}")
            }
        }.start()
    }


    private val uiModePollHandler = Handler(Looper.getMainLooper())
    private val uiModePollRunnable = object : Runnable {
        override fun run() {
            fetchZoomValueAndUpdateUI()
            uiModePollHandler.postDelayed(this, 4000) // poll every 4 seconds
        }
    }



    private fun updateCardSizes() {

        rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size())
    }



    private fun handleRecommendationSelection(rec: Recommendation) {
        try {
            cleanupPreviousTrailer()
            focusedMovie = null
            currentTrailerId = null
            bannerTitle?.text = rec.title
            bannerDescription?.text = rec.description
            val ratingValue = rec.rating ?: 0f
            if (ratingValue > 0) {
                val rating = (ratingValue / 2.0).coerceIn(0.0, 5.0)
                bannerRatingBar?.rating = rating.toFloat()
                bannerRatingBar?.visibility = View.VISIBLE
            } else {
                bannerRatingBar?.visibility = View.GONE
            }
            val imageUrl = rec.backdrop_image ?: rec.poster_image
            loadImage(imageUrl)
            scheduleRecommendationTrailer(rec)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error handling recommendation selection: ${e.message}", e)
        }
    }

    private fun handleMovieClick(movie: Movie) {
        // If a dialog is already showing, do nothing.
        if (parentFragmentManager.findFragmentByTag("MovieDetailsDialog") != null) {
            return
        }

        // 1. Log the final hover duration for the item being clicked.
        val now = System.currentTimeMillis()
        if (isContentItem(currentHoverItem)) {
            val dur = now - lastHoverStartTime
            if (dur > 50) {
                FirebaseLogger.logInteraction(
                    actionType = "hover",
                    screenContext = "Home",
                    focusedItem = currentHoverItem,
                    hoverDuration = dur / 1000f,
                    timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                    consecutiveActionCount = SequenceTracker.increment("hover")
                )
            }
        }
        currentHoverItem = null

        // 2. Log the "more_info" click event.
        FirebaseLogger.logInteraction(
            actionType = "click",
            screenContext = "Home",
            focusedItem = movie,
            timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
            consecutiveActionCount = SequenceTracker.increment("click"),
            clickType = "more_info"
        )

        stopTrailerPlayback()
        cleanupPreviousTrailer()
        val recommendedMovies = (0 until recommendationAdapter.size())
            .mapNotNull { recommendationAdapter.get(it) as? Movie }
            .filter { it != movie }
        val dialog = MovieDetailsDialogFragment(movie, allMovies, recommendedMovies, null)
            .setDeeplinkListener(object : MovieDetailsDialogFragment.DeeplinkListener {
                override fun onPlayClicked(movie: Movie) {
                    launchDeeplink(movie)
                }
            })

        dialog.onDialogDismiss = {
            focusedMovie?.let { handleMovieSelection(it) }
        }
        dialog.show(parentFragmentManager, "MovieDetailsDialog")
    }


    private fun launchNetflixApp(homepage: String?) {
        try {
            val netflixIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(homepage ?: "https://www.netflix.com/")
                setPackage("com.netflix.mediaclient")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(netflixIntent)
            Log.d("MovieFragment", "Launched Netflix app successfully")
        } catch (e: Exception) {
            Log.w("MovieFragment", "Netflix app not found, trying browser fallback: ${e.message}")
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(homepage ?: "https://www.netflix.com/")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(browserIntent)
            } catch (fallbackError: Exception) {
                Log.e("MovieFragment", "Failed to open Netflix in browser: ${fallbackError.message}")
            }
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

    private fun scheduleTrailerPlayback(movie: Movie) {
        try {
            Log.d("MovieFragment", "📽️ Scheduling trailer for: ${movie.title}, trailer: ${movie.trailer}")
            val url = movie.trailer?.takeIf { it.contains("youtube") || it.contains("youtu.be") } ?: return
            val trailerId = when {
                url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                url.contains("youtu.be/") -> url.substringAfterLast("/")
                else -> null
            } ?: return
            currentTrailerId = trailerId
            trailerRunnable = Runnable {
                if (focusedMovie == movie && currentTrailerId == trailerId) {
                    Log.d("MovieFragment", "🟢 Running trailerRunnable for ${movie.title}, trailerId: $trailerId")
                    try {
                        playTrailer(trailerId, mute = true)
                    } catch (e: Exception) {
                        Log.e("MovieFragment", "Error playing trailer: ${e.message}", e)
                        fallbackToImage()
                    }
                } else {
                    Log.d("MovieFragment", "⚠️ Trailer aborted, focus mismatch")
                }
            }
            trailerHandler.postDelayed(trailerRunnable!!, TRAILER_DELAY)
            Log.d("MovieFragment", "⏳ Trailer scheduled after ${TRAILER_DELAY / 1000} seconds")
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
            html, body { width: 100%; height: 100%; background: #000; margin: 0; padding: 0; }
            #player { width: 100%; height: 100vh; }
        </style>
    </head>
    <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
            var player;
            window.player = player;
            function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                    height: '100%',
                    width: '100%',
                    videoId: '$trailerId',
                    playerVars: { autoplay: 1, mute: $muteParam, enablejsapi: 1 },
                    events: {
                        onStateChange: onPlayerStateChange,
                        onReady: onPlayerReady
                    }
                });
                window.player = player;
            }
            function onPlayerReady(event) {
                window.player = player;
            }
            function onPlayerStateChange(event) {
                if (event.data === YT.PlayerState.ENDED) {
                    if (window.Android && typeof Android.onTrailerProgress === "function") {
                        Android.onTrailerProgress(100);
                    }
                }
            }
            window.pauseVideo = function() {
                if (player && typeof player.pauseVideo === 'function') {
                    player.pauseVideo();
                }
            };
            setInterval(function() {
                if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                    var c = player.getCurrentTime();
                    var t = player.getDuration();
                    if (t > 0) {
                        var pct = Math.floor((c / t) * 100);
                        if (window.Android && typeof Android.onTrailerProgress === "function") {
                            Android.onTrailerProgress(pct);
                        }
                    }
                }
            }, 1000);
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

    private fun resetToImageMode() {
        try {
            bannerWebView?.let { webView ->
                if (webView.visibility == View.VISIBLE) {
                    webView.evaluateJavascript("if(typeof player !== 'undefined' && player.pauseVideo) { player.pauseVideo(); }", null)
                    Handler(Looper.getMainLooper()).postDelayed({
                        webView.loadUrl("about:blank")
                        webView.visibility = View.GONE
                    }, 100)
                }
            }
            trailerHandler.removeCallbacksAndMessages(null)
            trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
            bannerImageView?.visibility = View.VISIBLE
            bannerOverlay?.visibility = View.VISIBLE
            unmuteButton?.visibility = View.GONE
            resizeBanner(BANNER_HEIGHT_IMAGE)
            currentTrailerId = null
            isMuted = true
            trailerStartTime = 0L
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error resetting to image mode: ${e.message}", e)
        }
    }
    private fun updateUIForSelectedItem(item: Any?) {
        try {
            when (item) {
                is Movie -> handleMovieSelection(item)
                is Recommendation -> handleRecommendationSelection(item)
                is CreateSocialRoomAction -> handleCreateSocialRoomSelection(item)
                is JoinSocialRoomAction -> handleJoinSocialRoomSelection(item)
                else -> { /* Header selection is handled by onRowSelected */ }
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in item selection UI update: ${e.message}", e)
        }
    }
    private fun updateBannerContent(movie: Movie) {
        try {
            bannerTitle?.text = movie.title ?: "Unknown Title"

            // Use the new banner-specific helper function
            val platformSpannable = PlatformHelper.getSpannableStringForBanner(requireContext(), movie.studio)

            val descriptionBuilder = SpannableStringBuilder()
            descriptionBuilder.append(platformSpannable)
            descriptionBuilder.append("\n")
            descriptionBuilder.append(movie.description ?: "No description available")
            bannerDescription?.text = descriptionBuilder

            val rating = ((movie.rating ?: 0.0) / 2.0).coerceIn(0.0, 5.0)
            if (rating > 0) {
                bannerRatingBar?.rating = rating.toFloat()
                bannerRatingBar?.visibility = View.VISIBLE
            } else {
                bannerRatingBar?.visibility = View.GONE
            }
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
                    context?.let {
                        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                        Glide.with(it)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade(factory))
                            .into(imageView)
                    }
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
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
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
            val fadeAnimation = AlphaAnimation(1f, 0f).apply {
                duration = 300
                fillAfter = false
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation?) {
                        view.visibility = View.GONE
                        view.alpha = 1f
                    }
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
            view.startAnimation(fadeAnimation)
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in fadeOut animation: ${e.message}", e)
            view.visibility = View.GONE
            view.alpha = 1f
        }
    }

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
            stopTrailerPlayback()
            cleanupPreviousTrailer()
            AlertDialog.Builder(requireContext())
                .setTitle("Watch on")
                .setItems(movie.deeplinks.toTypedArray()) { _, which ->
                    val selectedLink = movie.deeplinks.getOrNull(which)
                    if (!selectedLink.isNullOrBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(selectedLink)
                        }
                        startActivity(intent)
                    } else {
                        DeepLinkHelper.launchUrl(requireContext(), movie.homepage)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    Log.d("MovieFragment", "Deeplink dialog dismissed")
                }
                .show()
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error showing deeplink dialog: ${e.message}", e)
        }
    }

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
    private fun handleItemClick(item: Any?) {
        try {
            when (item) {
                is Movie -> handleMovieClick(item)
                is CreateSocialRoomAction -> handleCreateSocialRoomClick(item)
                is JoinSocialRoomAction -> handleJoinSocialRoomClick(item)
            }
        } catch (e: Exception) {
            Log.e("MovieFragment", "Error in item click: ${e.message}", e)
        }
    }
    private fun cleanupPreviousTrailer() {
        trailerHandler.removeCallbacksAndMessages(null)
        trailerLoadTimeout?.let { trailerHandler.removeCallbacks(it) }
        trailerRunnable = null
        trailerLoadTimeout = null
        stopTrailerAndResetBanner()
    }

    private fun logPreviousTrailerDuration() {
        if (trailerStartTime != 0L && focusedMovie != null) {
            FirebaseLogger.logInteraction(
                actionType = "playback_abandon",
                screenContext = "Playback",
                focusedItem = focusedMovie!!,
                timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                consecutiveActionCount = SequenceTracker.increment("playback_abandon"),
                playbackPosition = lastTrailerProgress.toLong()
            )
        }
        trailerStartTime = 0L
        currentTrailerId = null
        lastHoveredMovieTitle = null
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
                when {
                    platform?.contains("Netflix", ignoreCase = true) == true ->
                        setPackage("com.netflix.mediaclient")
                    platform?.contains("Prime", ignoreCase = true) == true ->
                        setPackage("com.amazon.cloud9")
                }
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w("MovieFragment", "Could not launch specific app ($platform), falling back to generic intent. Error: ${e.message}")
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(fallbackIntent)
                } catch (fallbackError: Exception) {
                    Log.e("MovieFragment", "Fallback launch also failed: ${fallbackError.message}")
                }
            }
        }
    }
}