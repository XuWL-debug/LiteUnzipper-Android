package com.lite.unzipper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lite.unzipper.archive.ArchiveExtractor
import com.lite.unzipper.archive.ArchiveType
import com.lite.unzipper.archive.getDisplayName
import com.lite.unzipper.databinding.ActivityMainBinding
import com.lite.unzipper.settings.AppSettings
import com.lite.unzipper.ui.FileItem
import com.lite.unzipper.ui.FileListAdapter
import com.lite.unzipper.ui.OppoViewSeamlessHelper
import com.lite.unzipper.ui.PasswordDialog
import com.lite.unzipper.ui.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: FileListAdapter

    /** 当前是否正在进行密码重试（true 表示是 resumeAfterPasswordRetry 分支） */
    private var retryingWithPassword: Boolean = false

    /** 选择压缩包文件（支持多选） */
    private val pickArchives = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            lifecycleScope.launch {
                val items = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val name = contentResolver.getDisplayName(uri) ?: return@mapNotNull null
                        val size = getUriSize(uri)
                        val type = ArchiveType.fromFileName(name)
                        val isEncrypted = ArchiveExtractor.isEncrypted(
                            applicationContext, uri, name
                        )
                        FileItem(uri, name, size, type,
                            isEncrypted = isEncrypted, isSelected = true)
                    }
                }
                viewModel.addFiles(items)
            }
        }
    }

    /** 选择解压目标目录 */
    private val pickTargetDir = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val targetDir = DocumentFile.fromTreeUri(this, uri)
            if (targetDir != null) {
                viewModel.extractBatch(targetDir)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 应用暗色模式
        settings = AppSettings(this)
        settings.applyDarkMode()

        // 液态玻璃沉浸模式：让背景延伸到状态栏+导航栏，配合透明状态栏/导航栏实现苹果级玻璃
        enableEdgeToEdgeGlass()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupButtons()
        handleSendIntent(intent)
        observeViewModel()

        // 启动时的丝滑进场动画
        binding.root.post {
            playEnterAnimations()
        }
    }

    /** 沉浸式：内容延伸到系统栏之后，为玻璃效果提供素材 */
    private fun enableEdgeToEdgeGlass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                window.decorView.systemUiVisibility
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    /** 启动进场动画：顶栏下落、Dock 上升、列表淡入，顺序错开更像苹果 */
    private fun playEnterAnimations() {
        val easeOut = android.view.animation.AnimationUtils.loadAnimation(
            this, R.anim.topbar_enter
        )
        binding.appBarLayout.startAnimation(easeOut)

        binding.root.postDelayed({
            val dockAnim = android.view.animation.AnimationUtils.loadAnimation(
                this, R.anim.dock_enter
            )
            binding.dockBar.startAnimation(dockAnim)
        }, 120)
    }

    override fun onResume() {
        super.onResume()
        // 设置页变更后重新应用暗色模式
        settings.applyDarkMode()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            OppoViewSeamlessHelper.startActivityWithSeamless(binding.toolbar, intent)
            true
        }
        R.id.action_dark_mode -> {
            showDarkModeDialog()
            true
        }
        R.id.action_threads -> {
            showThreadCountDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendIntent(intent)
    }

    private fun setupRecyclerView() {
        adapter = FileListAdapter { item ->
            viewModel.toggleSelection(item)
        }
        binding.fileList.layoutManager = LinearLayoutManager(this)
        binding.fileList.adapter = adapter
        // iOS 风格列表交错进场动画（每个 item 延迟 13% 依次出现）
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(
            this, R.anim.layout_items_enter
        )
        binding.fileList.layoutAnimation = controller
    }

    private fun setupButtons() {
        val mimeTypes = arrayOf(
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-bzip2",
            "application/x-xz",
            "application/octet-stream",
            "*/*"
        )
        val pickAction: (View) -> Unit = { pickArchives.launch(mimeTypes) }
        binding.btnSelectFile.setOnClickListener(pickAction)
        binding.btnEmptySelect?.setOnClickListener(pickAction)

        binding.btnExtract.setOnClickListener {
            retryingWithPassword = false
            viewModel.startExtraction()
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancelExtraction()
        }

        binding.btnShowFailures.setOnClickListener {
            val state = viewModel.state.value
            if (state is MainViewModel.ExtractState.BatchSuccess && state.failureDetails.isNotEmpty()) {
                showFailureDetails(state.failureDetails)
            }
        }

        // 所有按钮统一添加按压回弹动画（iOS 风格）
        listOfNotNull(
            binding.btnSelectFile,
            binding.btnExtract,
            binding.btnCancel,
            binding.btnShowFailures,
            binding.btnEmptySelect
        ).forEach { view ->
            addPressReleaseAnimation(view)
        }
    }

    /** 通用按压回弹：按下缩到 0.94，抬起过冲回弹到 1.0 */
    private fun addPressReleaseAnimation(view: android.view.View) {
        val pressInterpolator = android.view.animation.AccelerateInterpolator(1.5f)
        val releaseInterpolator = android.view.animation.OvershootInterpolator(2.4f)
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .setDuration(90)
                        .setInterpolator(pressInterpolator)
                        .start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(360)
                        .setInterpolator(releaseInterpolator)
                        .start()
                }
            }
            false
        }
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            lifecycleScope.launch {
                val name = contentResolver.getDisplayName(uri) ?: "未知文件"
                val size = getUriSize(uri)
                val type = ArchiveType.fromFileName(name)
                val isEncrypted = withContext(Dispatchers.IO) {
                    ArchiveExtractor.isEncrypted(applicationContext, uri, name)
                }
                val item = FileItem(uri, name, size, type,
                    isEncrypted = isEncrypted, isSelected = true)
                viewModel.addFiles(listOf(item))
            }
        }
    }

    private fun observeViewModel() {
        // 已选文件列表
        viewModel.selectedFiles.observe(this) { files ->
            adapter.submitList(files.toList())
            updateEmptyState(files.isEmpty())
            updateBottomBar(files)
        }

        // 解压状态
        viewModel.state.observe(this) { state ->
            when (state) {
                is MainViewModel.ExtractState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCancel.visibility = View.GONE
                    binding.btnExtract.isEnabled = viewModel.selectedFiles.value.orEmpty().any { it.isSelected }
                    binding.btnShowFailures.visibility = View.GONE
                }
                is MainViewModel.ExtractState.Running -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnExtract.isEnabled = false
                    binding.btnShowFailures.visibility = View.GONE
                    if (state.batchTotal > 1) {
                        binding.statusText.text = getString(
                            R.string.extracting_batch,
                            state.batchIndex + 1, state.batchTotal, state.currentMessage
                        )
                    } else {
                        binding.statusText.text = getString(R.string.extracting, state.currentMessage)
                    }
                    if (state.total > 0) {
                        binding.progressBar.isIndeterminate = false
                        binding.progressBar.max = state.total.toInt()
                        binding.progressBar.progress = state.current.toInt()
                    } else {
                        binding.progressBar.isIndeterminate = true
                    }
                }
                is MainViewModel.ExtractState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCancel.visibility = View.GONE
                    binding.btnExtract.isEnabled = viewModel.selectedFiles.value.orEmpty().any { it.isSelected }
                    binding.btnShowFailures.visibility = View.GONE
                    binding.statusText.text = getString(R.string.extract_success, state.count)
                }
                is MainViewModel.ExtractState.BatchSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCancel.visibility = View.GONE
                    binding.btnExtract.isEnabled = viewModel.selectedFiles.value.orEmpty().any { it.isSelected }
                    binding.statusText.text = getString(
                        R.string.extract_batch_success,
                        state.success, state.failure
                    )
                    if (state.failureDetails.isNotEmpty()) {
                        binding.btnShowFailures.visibility = View.VISIBLE
                    } else {
                        binding.btnShowFailures.visibility = View.GONE
                    }
                }
                is MainViewModel.ExtractState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCancel.visibility = View.GONE
                    binding.btnExtract.isEnabled = viewModel.selectedFiles.value.orEmpty().any { it.isSelected }
                    binding.btnShowFailures.visibility = View.GONE
                    binding.statusText.text = getString(R.string.extract_failed, state.message)
                }
            }
        }

        // 密码请求事件（首次检测加密或密码错误重试）
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.passwordRequest.collect { _ ->
                    showPasswordDialog()
                }
            }
        }

        // 选目录事件
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pickTargetDirRequest.collect {
                    pickTargetDir.launch(null)
                }
            }
        }
    }

    private fun showPasswordDialog() {
        val dialog = PasswordDialog()
        dialog.setCallback { password ->
            // 判断是首次密码提供还是重试分支
            if (retryingWithPassword) {
                viewModel.resumeAfterPasswordRetry(password)
            } else {
                retryingWithPassword = true
                viewModel.onPasswordProvided(password)
            }
        }
        dialog.show(supportFragmentManager, "password")
    }

    private fun showFailureDetails(details: List<String>) {
        val message = details.take(20).joinToString("\n\n") +
                if (details.size > 20) "\n\n... 另外 ${details.size - 20} 条省略" else ""
        MaterialAlertDialogBuilder(this)
            .setTitle("失败详情")
            .setMessage(message)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    /** 暗色模式切换对话框：跟随系统 / 浅色 / 深色 */
    private fun showDarkModeDialog() {
        val labels = arrayOf(
            getString(R.string.dark_mode_system_short),
            getString(R.string.dark_mode_light_short),
            getString(R.string.dark_mode_dark_short)
        )
        val current = when (settings.darkMode) {
            com.lite.unzipper.settings.DarkMode.SYSTEM -> 0
            com.lite.unzipper.settings.DarkMode.LIGHT -> 1
            com.lite.unzipper.settings.DarkMode.DARK -> 2
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_dark_mode)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                val mode = when (which) {
                    0 -> com.lite.unzipper.settings.DarkMode.SYSTEM
                    1 -> com.lite.unzipper.settings.DarkMode.LIGHT
                    else -> com.lite.unzipper.settings.DarkMode.DARK
                }
                settings.setDarkMode(mode)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 线程数调节对话框：1 / 2 / 3 / 4 */
    private fun showThreadCountDialog() {
        val current = settings.threadCount
        val max = 4
        val labels = (1..max).map { "$it 线程" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.thread_count_dialog_title)
            .setSingleChoiceItems(labels, current - 1) { dialog, which ->
                val count = which + 1
                settings.threadCount = count
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.fileList.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateBottomBar(files: List<FileItem>) {
        val selected = files.count { it.isSelected }
        val isRunning = viewModel.state.value is MainViewModel.ExtractState.Running
        binding.btnExtract.isEnabled = selected > 0 && !isRunning
        // 重新运行列表进场动画（有新文件被加入时，给用户丝滑反馈）
        if (files.isNotEmpty() && binding.fileList.adapter?.itemCount ?: 0 > 0) {
            binding.fileList.scheduleLayoutAnimation()
        }
    }

    private fun getUriSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) size = it.getLong(0)
        }
        return size
    }
}
