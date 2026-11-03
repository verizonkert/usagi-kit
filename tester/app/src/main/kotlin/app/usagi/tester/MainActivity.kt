package app.usagi.tester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.model.*

sealed interface Screen {
    data object Home : Screen
    data object Details : Screen
    data class Reader(val chapter: MangaChapter) : Screen
}

data class AppErrorInfo(
    val title: String,
    val message: String,
    val stackTrace: String,
    val sourceName: String? = null
)

class MainActivity : ComponentActivity() {
    private val loaderContext: MangaLoaderContext = TestMangaLoaderContext()
    private lateinit var pluginLoader: MangaPluginLoader

    private var onJarPicked: ((android.net.Uri) -> Unit)? = null

    private val pickJarLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onJarPicked?.invoke(it) }
    }

    private fun getFileName(uri: android.net.Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        return name ?: uri.lastPathSegment ?: "Plugin.jar"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pluginLoader = MangaPluginLoader(this)
        val imageLoader = coil.ImageLoader.Builder(this)
            .okHttpClient(loaderContext.httpClient)
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    onPrimary = Color(0xFF381E72),
                    primaryContainer = Color(0xFF4F378B),
                    surface = Color(0xFF1E1B24),
                    background = Color(0xFF141218),
                    onSurface = Color(0xFFE6E0E9),
                    onBackground = Color(0xFFE6E0E9),
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UsagiAppCatalogScreen(
                        context = loaderContext,
                        pluginLoader = pluginLoader,
                        onPickFileClick = { callback ->
                            onJarPicked = { uri ->
                                val realName = getFileName(uri)
                                callback(uri, realName)
                            }
                            pickJarLauncher.launch(
                                arrayOf(
                                    "application/java-archive",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, InternalParsersApi::class)
@Composable
fun UsagiAppCatalogScreen(
    context: MangaLoaderContext,
    pluginLoader: MangaPluginLoader,
    onPickFileClick: ((android.net.Uri, String) -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pluginLoaded by remember { mutableStateOf(false) }
    var pluginName by remember { mutableStateOf("No Plugin Loaded") }
    var availableSources by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSourceName by remember { mutableStateOf<String?>(null) }
    var activeParser by remember { mutableStateOf<MangaParser?>(null) }

    // Catalog state
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var selectedManga by remember { mutableStateOf<Manga?>(null) }
    var chapters by remember { mutableStateOf<List<MangaChapter>>(emptyList()) }
    var readerPages by remember { mutableStateOf<List<String>>(emptyList()) }

    // Pagination / Infinite scroll state
    var currentOffset by remember { mutableIntStateOf(0) }
    var hasMoreItems by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // Filter & Sort state
    var selectedSortOrder by remember { mutableStateOf(SortOrder.POPULARITY) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStateFilter by remember { mutableStateOf<MangaState?>(null) }
    var selectedTags by remember { mutableStateOf<Set<MangaTag>>(emptySet()) }
    var selectedExcludeTags by remember { mutableStateOf<Set<MangaTag>>(emptySet()) }
    var selectedRatings by remember { mutableStateOf<Set<ContentRating>>(emptySet()) }
    var selectedDemographics by remember { mutableStateOf<Set<Demographic>>(emptySet()) }
    var selectedContentTypes by remember { mutableStateOf<Set<ContentType>>(emptySet()) }
    var selectedLocale by remember { mutableStateOf<Locale?>(null) }
    var selectedOriginalLocale by remember { mutableStateOf<Locale?>(null) }
    var selectedYear by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterOptions by remember { mutableStateOf<MangaListFilterOptions?>(null) }

    var isInitialLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Initializing plugin...") }
    val contextAndroid = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var appError by remember { mutableStateOf<AppErrorInfo?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }

    fun reportError(title: String, throwable: Throwable, source: String? = selectedSourceName) {
        throwable.printStackTrace()
        android.util.Log.e("UsagiTester", "Error: $title", throwable)
        val sw = java.io.StringWriter()
        throwable.printStackTrace(java.io.PrintWriter(sw))
        val fullTrace = sw.toString()
        val msg = "${throwable::class.java.simpleName}: ${throwable.message ?: throwable.cause?.message ?: throwable.toString()}"
        appError = AppErrorInfo(
            title = title,
            message = msg,
            stackTrace = fullTrace,
            sourceName = source
        )
        statusMessage = "Error: $msg"
    }

    fun copyErrorToClipboard(error: AppErrorInfo) {
        val report = buildString {
            appendLine("=== Usagi Manga Explorer Error Report ===")
            appendLine("Title: ${error.title}")
            if (error.sourceName != null) {
                appendLine("Source: ${error.sourceName}")
            }
            appendLine("Message: ${error.message}")
            appendLine("----------------- STACKTRACE -----------------")
            appendLine(error.stackTrace)
        }
        clipboardManager.setText(AnnotatedString(report))
        Toast.makeText(contextAndroid, "Copied error & stacktrace to clipboard!", Toast.LENGTH_SHORT).show()
    }

    val gridState = rememberLazyGridState()

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = when (currentScreen) {
            is Screen.Reader -> Screen.Details
            else -> Screen.Home
        }
    }

    // Function to reload catalog from offset 0
    fun loadCatalog(reset: Boolean = true) {
        val parser = activeParser ?: return
        coroutineScope.launch {
            if (reset) {
                isInitialLoading = true
                currentOffset = 0
                hasMoreItems = true
            } else {
                if (isLoadingMore || !hasMoreItems) return@launch
                isLoadingMore = true
            }

            try {
                val offsetToFetch = if (reset) 0 else currentOffset
                val filter = MangaListFilter(
                    query = searchQuery.ifBlank { null },
                    states = selectedStateFilter?.let { setOf(it) } ?: emptySet(),
                    tags = selectedTags,
                    tagsExclude = selectedExcludeTags,
                    contentRating = selectedRatings,
                    demographics = selectedDemographics,
                    types = selectedContentTypes,
                    locale = selectedLocale,
                    originalLocale = selectedOriginalLocale,
                    year = selectedYear
                )

                val newItems = withContext(Dispatchers.IO) {
                    parser.getList(
                        offset = offsetToFetch,
                        order = selectedSortOrder,
                        filter = filter
                    )
                }

                if (reset) {
                    mangaList = newItems
                } else {
                    val existingIds = mangaList.map { it.id }.toSet()
                    val distinctNew = newItems.filter { it.id !in existingIds }
                    mangaList = mangaList + distinctNew
                }

                if (newItems.isEmpty()) {
                    hasMoreItems = false
                } else {
                    currentOffset = offsetToFetch + newItems.size
                }
                statusMessage = "Showing ${mangaList.size} manga from ${parser.source.name}"
                appError = null
            } catch (e: Throwable) {
                reportError("Fetch failed", e)
            } finally {
                isInitialLoading = false
                isLoadingMore = false
            }
        }
    }

    // Detect when user scrolls near the end to trigger infinite scroll
    LaunchedEffect(gridState.canScrollForward, mangaList.size) {
        if (!gridState.canScrollForward && mangaList.isNotEmpty() && hasMoreItems && !isLoadingMore && !isInitialLoading) {
            loadCatalog(reset = false)
        }
    }

    // Startup: dynamically load whatever plugin is available
    LaunchedEffect(Unit) {
        val loadedJar = withContext(Dispatchers.IO) {
            pluginLoader.loadDefaultPluginFromAssets()
        }
        if (loadedJar != null) {
            pluginLoaded = true
            pluginName = loadedJar
            val sources = pluginLoader.getAvailableSources()
            availableSources = sources
            statusMessage = "Loaded $loadedJar (${sources.size} sources)"
            val firstSource = sources.firstOrNull()

            if (firstSource != null) {
                try {
                    val parser = pluginLoader.createParser(firstSource, context)
                    activeParser = parser
                    selectedSourceName = firstSource
                    selectedSortOrder = parser.availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
                    try {
                        val opts = withContext(Dispatchers.IO) { parser.getFilterOptions() }
                        filterOptions = opts
                        android.util.Log.d("UsagiTester", "Loaded filterOptions: tags=${opts.availableTags.size}, states=${opts.availableStates.size}, ratings=${opts.availableContentRating.size}, demographics=${opts.availableDemographics.size}")
                    } catch (e: Throwable) {
                        android.util.Log.e("UsagiTester", "Failed to fetch filterOptions", e)
                    }
                    loadCatalog(reset = true)
                } catch (e: Throwable) {
                    reportError("Error initializing source $firstSource", e, firstSource)
                }
            }
        } else {
            statusMessage = "Tap 'Load JAR' to pick an extension from disk"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (val s = currentScreen) {
                                Screen.Home -> "Usagi Manga Explorer"
                                Screen.Details -> selectedManga?.title ?: "Manga Details"
                                is Screen.Reader -> s.chapter.title ?: "Chapter ${s.chapter.number}"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentScreen == Screen.Home) {
                            Text(
                                text = "JAR: $pluginName • Source: ${selectedSourceName ?: "None"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (currentScreen is Screen.Reader) {
                            Text(
                                text = "${readerPages.size} pages",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentScreen != Screen.Home) {
                        IconButton(onClick = {
                            currentScreen = when (currentScreen) {
                                is Screen.Reader -> Screen.Details
                                else -> Screen.Home
                            }
                        }) {
                            Text("←", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Home) {
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("UsagiTester", "Filters button clicked! filterOptions=$filterOptions")
                                showFilterSheet = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("⚙ Filters", fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                onPickFileClick { uri, realName ->
                                    coroutineScope.launch {
                                        isInitialLoading = true
                                        statusMessage = "Loading $realName..."
                                        val success = withContext(Dispatchers.IO) {
                                            pluginLoader.loadPluginFromUri(uri)
                                        }
                                        if (success) {
                                            pluginLoaded = true
                                            pluginName = realName
                                            val sources = pluginLoader.getAvailableSources()
                                            availableSources = sources
                                            val first = sources.firstOrNull()
                                            selectedSourceName = first
                                            if (first != null) {
                                                try {
                                                    val parser = pluginLoader.createParser(first, context)
                                                    activeParser = parser
                                                    selectedSortOrder = parser.availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
                                                    try {
                                                        filterOptions = withContext(Dispatchers.IO) { parser.getFilterOptions() }
                                                    } catch (_: Throwable) {}
                                                    loadCatalog(reset = true)
                                                } catch (e: Throwable) {
                                                    reportError("Failed to initialize source $first", e, first)
                                                }
                                            }
                                            statusMessage = "Loaded $realName (${sources.size} sources)"
                                        } else {
                                            val err = pluginLoader.lastError ?: "Unknown error"
                                            reportError("Failed to load plugin $realName", RuntimeException(err))
                                        }
                                        isInitialLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("📁 Load JAR", fontSize = 12.sp, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val screen = currentScreen) {
                Screen.Home -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Source Switcher & Quick Search
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                            .clickable(enabled = appError != null) {
                                                showErrorDialog = true
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statusMessage,
                                            fontSize = 12.sp,
                                            color = if (appError != null) MaterialTheme.colorScheme.error else Color.LightGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (availableSources.isNotEmpty()) {
                                        var showSourceDialog by remember { mutableStateOf(false) }
                                        var sourceSearchQuery by remember { mutableStateOf("") }

                                        OutlinedButton(
                                            onClick = {
                                                sourceSearchQuery = ""
                                                showSourceDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${selectedSourceName ?: "Source"} (${availableSources.size})",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (showSourceDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showSourceDialog = false },
                                                title = {
                                                    Text(
                                                        "Select Source (${availableSources.size} available)",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                text = {
                                                    val filteredSources = remember(sourceSearchQuery, availableSources) {
                                                        if (sourceSearchQuery.isBlank()) availableSources
                                                        else availableSources.filter { it.contains(sourceSearchQuery, ignoreCase = true) }
                                                    }
                                                    Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                                                        OutlinedTextField(
                                                            value = sourceSearchQuery,
                                                            onValueChange = { sourceSearchQuery = it },
                                                            placeholder = { Text("Search among ${availableSources.size} sources...", fontSize = 12.sp) },
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            items(filteredSources.size) { index ->
                                                                val src = filteredSources[index]
                                                                val slNo = index + 1
                                                                Card(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable {
                                                                            showSourceDialog = false
                                                                            coroutineScope.launch {
                                                                                try {
                                                                                    val parser = pluginLoader.createParser(src, context)
                                                                                    activeParser = parser
                                                                                    selectedSourceName = src
                                                                                    selectedSortOrder = parser.availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
                                                                                    selectedStateFilter = null
                                                                                    selectedTags = emptySet()
                                                                                    selectedExcludeTags = emptySet()
                                                                                    selectedRatings = emptySet()
                                                                                    selectedDemographics = emptySet()
                                                                                    selectedContentTypes = emptySet()
                                                                                    selectedLocale = null
                                                                                    selectedOriginalLocale = null
                                                                                    selectedYear = 0
                                                                                    try {
                                                                                        val opts = withContext(Dispatchers.IO) { parser.getFilterOptions() }
                                                                                        filterOptions = opts
                                                                                        android.util.Log.d("UsagiTester", "Loaded filterOptions for $src: tags=${opts.availableTags.size}, states=${opts.availableStates.size}, ratings=${opts.availableContentRating.size}, demographics=${opts.availableDemographics.size}")
                                                                                    } catch (e: Throwable) {
                                                                                        android.util.Log.e("UsagiTester", "Failed to fetch filterOptions for $src", e)
                                                                                    }
                                                                                    loadCatalog(reset = true)
                                                                                } catch (e: Throwable) {
                                                                                    reportError("Error initializing source $src", e, src)
                                                                                }
                                                                            }
                                                                        },
                                                                    colors = CardDefaults.cardColors(
                                                                        containerColor = if (src == selectedSourceName) MaterialTheme.colorScheme.primaryContainer else Color(0xFF26232E)
                                                                    ),
                                                                    shape = RoundedCornerShape(6.dp)
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Text(
                                                                            text = "#$slNo",
                                                                            fontSize = 11.sp,
                                                                            color = Color.Gray,
                                                                            modifier = Modifier.width(42.dp)
                                                                        )
                                                                        Text(
                                                                            text = src,
                                                                            fontSize = 13.sp,
                                                                            fontWeight = if (src == selectedSourceName) FontWeight.Bold else FontWeight.Normal,
                                                                            color = Color.White
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = { showSourceDialog = false }) {
                                                        Text("Close")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                if (appError != null) {
                                    val err = appError!!
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚠️ ${err.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { appError = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = err.message,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { showErrorDialog = true },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("View Stacktrace", fontSize = 11.sp, color = MaterialTheme.colorScheme.onError)
                                                }
                                                OutlinedButton(
                                                    onClick = { copyErrorToClipboard(err) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("📋 Copy Stacktrace", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Search input bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search manga...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Button(
                                        onClick = { loadCatalog(reset = true) },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Search", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Active Filters Chips Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Active sort chip
                            SuggestionChip(
                                onClick = { showFilterSheet = true },
                                label = { Text("Sort: ${selectedSortOrder.name.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 11.sp) }
                            )

                            // Status filter chip
                            if (selectedStateFilter != null) {
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        selectedStateFilter = null
                                        loadCatalog(reset = true)
                                    },
                                    label = { Text("State: ${selectedStateFilter.toString()}", fontSize = 11.sp) },
                                    trailingIcon = { Text("✕", fontSize = 10.sp) }
                                )
                            }
                        }

                        // Grid with Infinite Scroll
                        if (mangaList.isNotEmpty()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(mangaList, key = { index, manga ->
                                    if (manga.url.isNotBlank()) "manga_${manga.url}_$index" else "manga_${manga.id}_$index"
                                }) { _, manga ->
                                    MangaGridCard(
                                        manga = manga,
                                        onClick = {
                                            val parser = activeParser ?: return@MangaGridCard
                                            coroutineScope.launch {
                                                isInitialLoading = true
                                                try {
                                                    val detailed = withContext(Dispatchers.IO) {
                                                        parser.getDetails(manga)
                                                    }
                                                    selectedManga = detailed
                                                    chapters = detailed.chapters ?: emptyList()
                                                    currentScreen = Screen.Details
                                                    appError = null
                                                } catch (e: Throwable) {
                                                    reportError("Error opening manga: ${manga.title}", e)
                                                } finally {
                                                    isInitialLoading = false
                                                }
                                            }
                                        }
                                    )
                                }

                                // Bottom Infinite Scroll loading indicator item
                                if (isLoadingMore) {
                                    item(key = "loading_more_progress_item") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (!isInitialLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No Manga Loaded", color = Color.Gray, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { loadCatalog(reset = true) }) {
                                        Text("Fetch Manga Catalog")
                                    }
                                }
                            }
                        }
                    }

                    // Filter Options Bottom Sheet
                    if (showFilterSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showFilterSheet = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.85f)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filter & Sort",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val totalFilters = (if (selectedStateFilter != null) 1 else 0) +
                                            selectedTags.size +
                                            selectedExcludeTags.size +
                                            selectedRatings.size +
                                            selectedDemographics.size +
                                            selectedContentTypes.size +
                                            (if (selectedLocale != null) 1 else 0) +
                                            (if (selectedOriginalLocale != null) 1 else 0) +
                                            (if (selectedYear > 0) 1 else 0)
                                    if (totalFilters > 0) {
                                        Text(
                                            text = "$totalFilters active",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    val parser = activeParser
                                    val capabilities = parser?.filterCapabilities

                                    // 1. Sort Orders (Only if source defines available sort orders)
                                    val availableSorts = parser?.availableSortOrders ?: emptyList()
                                    if (availableSorts.isNotEmpty()) {
                                        Text(
                                            text = "Sorting order",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            availableSorts.forEach { order ->
                                                FilterChip(
                                                    selected = selectedSortOrder == order,
                                                    onClick = { selectedSortOrder = order },
                                                    label = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 2. Language / Locales (Only if source provides availableLocales)
                                    val availableLocales = filterOptions?.availableLocales?.toList() ?: emptyList()
                                    if (availableLocales.isNotEmpty()) {
                                        Text(
                                            text = "Language",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FilterChip(
                                                selected = selectedLocale == null,
                                                onClick = { selectedLocale = null },
                                                label = { Text("All languages", fontSize = 12.sp) }
                                            )
                                            availableLocales.forEach { loc ->
                                                val locLabel = loc.getDisplayName(loc).replaceFirstChar { it.uppercase() }.ifBlank { loc.toLanguageTag() }
                                                FilterChip(
                                                    selected = selectedLocale == loc,
                                                    onClick = { selectedLocale = if (selectedLocale == loc) null else loc },
                                                    label = { Text(locLabel, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 3. Original Language (Only if source filterCapabilities.isOriginalLocaleSupported)
                                    if (capabilities?.isOriginalLocaleSupported == true && availableLocales.isNotEmpty()) {
                                        Text(
                                            text = "Original language",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FilterChip(
                                                selected = selectedOriginalLocale == null,
                                                onClick = { selectedOriginalLocale = null },
                                                label = { Text("All languages", fontSize = 12.sp) }
                                            )
                                            availableLocales.forEach { loc ->
                                                val locLabel = loc.getDisplayName(loc).replaceFirstChar { it.uppercase() }.ifBlank { loc.toLanguageTag() }
                                                FilterChip(
                                                    selected = selectedOriginalLocale == loc,
                                                    onClick = { selectedOriginalLocale = if (selectedOriginalLocale == loc) null else loc },
                                                    label = { Text(locLabel, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 4. Genres / Tags (Include) (Only what exists in source filterOptions)
                                    val availableTags = filterOptions?.availableTags?.toList() ?: emptyList()
                                    if (availableTags.isNotEmpty()) {
                                        var showAllIncludeTags by remember { mutableStateOf(false) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Genres (${availableTags.size})",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (availableTags.size > 14) {
                                                Text(
                                                    text = if (showAllIncludeTags) "Less" else "More",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable { showAllIncludeTags = !showAllIncludeTags }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val tagsToDisplay = if (showAllIncludeTags) availableTags else availableTags.take(14)
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            tagsToDisplay.forEach { tag ->
                                                val isSelected = tag in selectedTags
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedTags = if (isSelected) {
                                                            selectedTags - tag
                                                        } else {
                                                            selectedExcludeTags = selectedExcludeTags - tag
                                                            if (capabilities?.isMultipleTagsSupported == false) {
                                                                setOf(tag)
                                                            } else {
                                                                selectedTags + tag
                                                            }
                                                        }
                                                    },
                                                    label = { Text(tag.title, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 5. Exclude Genres (Only if source capabilities allows isTagsExclusionSupported and source has tags)
                                    val isTagsExclusionSupported = capabilities?.isTagsExclusionSupported ?: (availableTags.isNotEmpty())
                                    if (isTagsExclusionSupported && availableTags.isNotEmpty()) {
                                        var showAllExcludeTags by remember { mutableStateOf(false) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Exclude genres",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (availableTags.size > 14) {
                                                Text(
                                                    text = if (showAllExcludeTags) "Less" else "More",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable { showAllExcludeTags = !showAllExcludeTags }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val excludeTagsToDisplay = if (showAllExcludeTags) availableTags else availableTags.take(14)
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            excludeTagsToDisplay.forEach { tag ->
                                                val isExcluded = tag in selectedExcludeTags
                                                FilterChip(
                                                    selected = isExcluded,
                                                    onClick = {
                                                        selectedExcludeTags = if (isExcluded) {
                                                            selectedExcludeTags - tag
                                                        } else {
                                                            selectedTags = selectedTags - tag
                                                            selectedExcludeTags + tag
                                                        }
                                                    },
                                                    label = { Text(tag.title, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 6. State (Only what exists in source filterOptions)
                                    val availableStates = filterOptions?.availableStates ?: emptySet()
                                    if (availableStates.isNotEmpty()) {
                                        Text(
                                            text = "State",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FilterChip(
                                                selected = selectedStateFilter == null,
                                                onClick = { selectedStateFilter = null },
                                                label = { Text("All", fontSize = 12.sp) }
                                            )
                                            availableStates.forEach { state ->
                                                val stateName = state.name.lowercase().replaceFirstChar { it.uppercase() }
                                                FilterChip(
                                                    selected = selectedStateFilter == state,
                                                    onClick = { selectedStateFilter = if (selectedStateFilter == state) null else state },
                                                    label = { Text(stateName, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 7. Content Rating (Only what exists in source filterOptions)
                                    val availableRatings = filterOptions?.availableContentRating ?: emptySet()
                                    if (availableRatings.isNotEmpty()) {
                                        Text(
                                            text = "Content rating",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            availableRatings.forEach { rating ->
                                                val isRatingSelected = rating in selectedRatings
                                                val ratingLabel = rating.name.lowercase().replaceFirstChar { it.uppercase() }
                                                FilterChip(
                                                    selected = isRatingSelected,
                                                    onClick = {
                                                        selectedRatings = if (isRatingSelected) {
                                                            selectedRatings - rating
                                                        } else {
                                                            selectedRatings + rating
                                                        }
                                                    },
                                                    label = { Text(ratingLabel, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 8. Demographics (Only what exists in source filterOptions)
                                    val availableDemographics = filterOptions?.availableDemographics ?: emptySet()
                                    if (availableDemographics.isNotEmpty()) {
                                        Text(
                                            text = "Demographics",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            availableDemographics.forEach { demo ->
                                                val isDemoSelected = demo in selectedDemographics
                                                val demoLabel = demo.name.lowercase().replaceFirstChar { it.uppercase() }
                                                FilterChip(
                                                    selected = isDemoSelected,
                                                    onClick = {
                                                        selectedDemographics = if (isDemoSelected) {
                                                            selectedDemographics - demo
                                                        } else {
                                                            selectedDemographics + demo
                                                        }
                                                    },
                                                    label = { Text(demoLabel, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 9. Content Types (Manga, Manhwa, Manhua - only if source provides availableContentTypes)
                                    val availableTypes = filterOptions?.availableContentTypes ?: emptySet()
                                    if (availableTypes.isNotEmpty()) {
                                        Text(
                                            text = "Content type",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            availableTypes.forEach { type ->
                                                val isTypeSelected = type in selectedContentTypes
                                                val typeLabel = type.name.lowercase().replaceFirstChar { it.uppercase() }
                                                FilterChip(
                                                    selected = isTypeSelected,
                                                    onClick = {
                                                        selectedContentTypes = if (isTypeSelected) {
                                                            selectedContentTypes - type
                                                        } else {
                                                            selectedContentTypes + type
                                                        }
                                                    },
                                                    label = { Text(typeLabel, fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // 10. Publication Year (Only if source filterCapabilities.isYearSupported)
                                    if (capabilities?.isYearSupported == true) {
                                        val currentCalYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                        val minYear = 1960
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Year",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (selectedYear == 0) "Any" else selectedYear.toString(),
                                                fontSize = 13.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = if (selectedYear == 0) (currentCalYear + 1).toFloat() else selectedYear.toFloat(),
                                            onValueChange = { value ->
                                                val intVal = value.toInt()
                                                selectedYear = if (intVal > currentCalYear) 0 else intVal
                                            },
                                            valueRange = minYear.toFloat()..(currentCalYear + 1).toFloat(),
                                            steps = (currentCalYear - minYear),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Action Buttons (Bottom Bar inside sheet)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedStateFilter = null
                                            selectedTags = emptySet()
                                            selectedExcludeTags = emptySet()
                                            selectedRatings = emptySet()
                                            selectedDemographics = emptySet()
                                            selectedContentTypes = emptySet()
                                            selectedLocale = null
                                            selectedOriginalLocale = null
                                            selectedYear = 0
                                            showFilterSheet = false
                                            loadCatalog(reset = true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Reset")
                                    }

                                    Button(
                                        onClick = {
                                            showFilterSheet = false
                                            loadCatalog(reset = true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Done")
                                    }
                                }
                            }
                        }
                    }
                }

                Screen.Details -> {
                    selectedManga?.let { manga ->
                        MangaDetailsView(
                            manga = manga,
                            chapters = chapters,
                            onChapterClick = { chapter ->
                                val parser = activeParser ?: return@MangaDetailsView
                                coroutineScope.launch {
                                    isInitialLoading = true
                                    try {
                                        val pages = withContext(Dispatchers.IO) {
                                            val rawPages = parser.getPages(chapter)
                                            rawPages.map { page ->
                                                try {
                                                    parser.getPageUrl(page)
                                                } catch (e: Exception) {
                                                    page.url
                                                }
                                            }
                                        }
                                        readerPages = pages
                                        currentScreen = Screen.Reader(chapter)
                                        appError = null
                                    } catch (e: Throwable) {
                                        reportError("Failed to load chapter pages", e)
                                    } finally {
                                        isInitialLoading = false
                                    }
                                }
                            }
                        )
                    }
                }

                is Screen.Reader -> {
                    MangaReaderView(
                        pages = readerPages,
                        chapter = screen.chapter
                    )
                }
            }

            if (isInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading...", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showErrorDialog && appError != null) {
        val err = appError!!
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Error Details",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(
                        onClick = { showErrorDialog = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (err.sourceName != null) {
                        Text(
                            text = "Source: ${err.sourceName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = err.message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Stacktrace:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 340.dp)
                            .background(Color(0xFF15131C), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = err.stackTrace,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFB4AB),
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { copyErrorToClipboard(err) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("📋 Copy Stacktrace", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        )
    }
}

/**
 * Usagi-styled Manga Grid Item with image cover, gradient overlay, and title badge.
 */
@Composable
fun MangaGridCard(
    manga: Manga,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26232E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cover = manga.largeCoverUrl ?: manga.coverUrl
            if (!cover.isNullOrEmpty()) {
                AsyncImage(
                    model = cover,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF322E3D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 36.sp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = manga.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (manga.state != null) {
                    Text(
                        text = manga.state.toString(),
                        fontSize = 10.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (manga.hasRating) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = Color(0xFFE5A00D),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "★ ${(manga.rating * 10).toInt() / 10f}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Usagi-styled Manga Details View with Cover, Info, Synopsis and Chapters list.
 */
@Composable
fun MangaDetailsView(
    manga: Manga,
    chapters: List<MangaChapter>,
    onChapterClick: (MangaChapter) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Cover + Info
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(185.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val cover = manga.largeCoverUrl ?: manga.coverUrl
                    if (!cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = manga.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF322E3D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 40.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = manga.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (manga.authors.isNotEmpty()) {
                        Text(
                            text = "Author: ${manga.authors.joinToString()}",
                            fontSize = 12.sp,
                            color = Color(0xFFD0BCFF)
                        )
                    }

                    if (manga.state != null) {
                        Text(
                            text = "Status: ${manga.state}",
                            fontSize = 12.sp,
                            color = Color(0xFF81C784)
                        )
                    }

                    if (manga.tags.isNotEmpty()) {
                        Text(
                            text = manga.tags.take(4).joinToString { it.title },
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Description
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Synopsis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = manga.description?.ifBlank { "No description provided." } ?: "No description provided.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Chapters Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chapters (${chapters.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Chapters List
        items(chapters) { chapter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChapterClick(chapter) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF26232E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title ?: "Chapter ${chapter.number}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "No. ${chapter.number}${if (chapter.scanlator != null) " • " + chapter.scanlator else ""}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = { onChapterClick(chapter) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Read",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Usagi Reader View showing the actual chapter pages in continuous scroll.
 */
@Composable
fun MangaReaderView(
    pages: List<String>,
    chapter: MangaChapter
) {
    if (pages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No pages found for this chapter", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(pages.size) { index ->
                val pageUrl = pages[index]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = pageUrl,
                        contentDescription = "Page ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                    Text(
                        text = "Page ${index + 1} of ${pages.size}",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
