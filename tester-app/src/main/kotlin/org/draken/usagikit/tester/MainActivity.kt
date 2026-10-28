package org.draken.usagikit.tester

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.draken.usagikit.UsagiKit
import org.draken.usagikit.model.UsagiSource
import org.draken.usagikit.tester.databinding.ActivityMainBinding
import org.draken.usagikit.tester.databinding.MangaDetailsBottomSheetBinding
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaListFilter
import tsuki.model.SortOrder
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MangaAdapter
    private var activeSource: UsagiSource? = null
    private var selectedSortOrder: SortOrder? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                copyAndLoadPlugin(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MangaAdapter { manga ->
            showMangaDetailsPanel(manga)
        }

        binding.rvManga.layoutManager = LinearLayoutManager(this)
        binding.rvManga.adapter = adapter

        binding.btnSelectPlugin.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            filePickerLauncher.launch(intent)
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text?.toString()?.trim().orEmpty()
            if (query.isNotEmpty()) {
                searchManga(query)
            } else {
                loadMangaFeed()
            }
        }

        binding.spinnerSources.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sources = UsagiKit.getAllSources()
                if (position in sources.indices) {
                    activeSource = sources[position]
                    setupSortChips()
                    loadMangaFeed()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        refreshSourcesSpinner()
    }

    private fun setupSortChips() {
        binding.chipGroupSort.removeAllViews()
        val source = activeSource ?: return
        val availableSorts = runCatching { UsagiKit.getAvailableSortOrders(source.name) }.getOrDefault(emptySet())
        if (availableSorts.isEmpty()) return

        availableSorts.forEach { sort ->
            val chip = Chip(this).apply {
                text = sort.name.lowercase().replaceFirstChar { it.uppercase() }
                isCheckable = true
                isChecked = (selectedSortOrder == sort) || (selectedSortOrder == null && sort == SortOrder.POPULARITY)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSortOrder = sort
                        loadMangaFeed()
                    }
                }
            }
            binding.chipGroupSort.addView(chip)
        }
    }

    private fun copyAndLoadPlugin(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.VISIBLE
            }
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Failed to open file stream")
                val pluginsDir = File(filesDir, "plugins")
                if (!pluginsDir.exists()) pluginsDir.mkdirs()

                val targetFile = File(pluginsDir, "plugin_${System.currentTimeMillis()}.jar")
                FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }

                val extension = UsagiKit.loadExtension(targetFile)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Loaded ${extension.sources.size} sources from ${extension.name}", Toast.LENGTH_LONG).show()
                    refreshSourcesSpinner()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshSourcesSpinner() {
        val sources = UsagiKit.getAllSources()
        if (sources.isNotEmpty()) {
            binding.tvEmpty.visibility = View.GONE
            val sourceTitles = sources.map { "${it.title} [${it.locale.ifEmpty { "all" }}]" }
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sourceTitles)
            binding.spinnerSources.adapter = spinnerAdapter
            activeSource = sources.first()
            setupSortChips()
            loadMangaFeed()
        } else {
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun loadMangaFeed() {
        val source = activeSource ?: return
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            try {
                val list = withContext(Dispatchers.IO) {
                    UsagiKit.getList(source.name, page = 1, sortOrder = selectedSortOrder)
                }
                binding.progressBar.visibility = View.GONE
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvEmpty.text = "No manga found."
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchManga(query: String) {
        val source = activeSource ?: return
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val list = withContext(Dispatchers.IO) {
                    UsagiKit.searchManga(source.name, page = 1, query = query)
                }
                binding.progressBar.visibility = View.GONE
                adapter.submitList(list)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMangaDetailsPanel(manga: Manga) {
        val source = activeSource ?: return
        val dialog = BottomSheetDialog(this)
        val sheetBinding = MangaDetailsBottomSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Initialize header with basic manga info
        sheetBinding.panelTitle.text = manga.title
        sheetBinding.panelAuthor.text = manga.authors.firstOrNull() ?: "Unknown Author"
        sheetBinding.panelCover.load(manga.coverUrl) { crossfade(true) }
        sheetBinding.panelDescription.text = manga.description ?: "Loading synopsis..."
        sheetBinding.panelState.text = manga.state?.name ?: "UNKNOWN"

        val chapterAdapter = ChapterAdapter { chapter ->
            loadChapterPages(chapter)
        }
        sheetBinding.panelRvChapters.layoutManager = LinearLayoutManager(this)
        sheetBinding.panelRvChapters.adapter = chapterAdapter

        sheetBinding.panelProgress.visibility = View.VISIBLE

        dialog.show()

        // Fetch full manga details & chapters asynchronously via UsagiKit Bridge
        lifecycleScope.launch {
            try {
                val fullDetails = withContext(Dispatchers.IO) {
                    UsagiKit.getDetails(source.name, manga)
                }
                sheetBinding.panelProgress.visibility = View.GONE
                sheetBinding.panelDescription.text = fullDetails.description?.takeIf { it.isNotBlank() } ?: "No synopsis provided."
                sheetBinding.panelCover.load(fullDetails.coverUrl ?: fullDetails.largeCoverUrl) { crossfade(true) }
                sheetBinding.panelState.text = fullDetails.state?.name ?: "STATUS"

                val chapters = fullDetails.chapters.orEmpty()
                sheetBinding.panelTotalChapters.text = "${chapters.size} Chapters available"
                chapterAdapter.submitList(chapters)

                // Populate tags
                sheetBinding.panelTagsGroup.removeAllViews()
                fullDetails.tags.take(12).forEach { tag ->
                    val chip = Chip(this@MainActivity).apply {
                        text = tag.title
                        isClickable = false
                    }
                    sheetBinding.panelTagsGroup.addView(chip)
                }

            } catch (e: Exception) {
                sheetBinding.panelProgress.visibility = View.GONE
                sheetBinding.panelDescription.text = "Failed to load details: ${e.message}"
            }
        }
    }

    private fun loadChapterPages(chapter: MangaChapter) {
        val source = activeSource ?: return
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val pages = withContext(Dispatchers.IO) {
                    UsagiKit.getPages(source.name, chapter)
                }
                binding.progressBar.visibility = View.GONE

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(chapter.title ?: "Chapter ${chapter.number}")
                    .setMessage("Successfully fetched ${pages.size} pages!\n\nFirst Page URL:\n${pages.firstOrNull()?.url}")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Pages failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
