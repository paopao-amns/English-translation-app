package com.paopao.englearn.ui.ocr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paopao.englearn.ui.components.LoadingOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onNavigateBack: () -> Unit,
    onTextReady: (String) -> Unit,
    viewModel: OcrViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.setMode(OcrMode.CAMERA)
        }
    }

    // Camera capture
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedBitmap = it
            viewModel.processImage(it)
        }
    }

    // ── Gallery → copy to local → crop ──────────────────
    // Two-step flow to avoid URI permission issues on some devices (e.g. MuMu):
    //  1. GetContent() returns a content:// URI only readable by our activity
    //  2. Copy it to a local cache file on IO thread → generate our own FileProvider URI
    //  3. Pass the local URI to CropImageContract — always readable
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    // Local copy of the source image handed to CropActivity. We own its lifecycle
    // and must delete it once the crop activity returns (success or cancel).
    var pendingInputFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> pendingImageUri = uri }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // The input temp file is no longer needed once cropping is done.
        pendingInputFile?.let { it.delete() }
        pendingInputFile = null

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            var outFile: File? = null
            try {
                val path = result.data?.getStringExtra(CropActivity.EXTRA_RESULT_PATH)
                if (path != null) {
                    outFile = File(path)
                    outFile.inputStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { bmp ->
                            capturedBitmap = bmp
                            viewModel.processImage(bmp)
                        }
                    }
                }
            } catch (e: Exception) {
                viewModel.setError("裁剪失败，请重试")
            } finally {
                // Output temp file is fully decoded into memory; clean it up.
                outFile?.delete()
            }
        }
    }

    // Copy selected image to local cache on IO dispatcher, then launch cropper.
    // We MUST copy to a local file because the content:// URI from GetContent()
    // grants only temporary read permission — the crop activity can't read it.
    LaunchedEffect(pendingImageUri) {
        pendingImageUri?.let { selectedUri ->
            // Holder for the input temp file so we can delete it after cropping.
            var tempFile: File? = null
            val localUri = withContext(Dispatchers.IO) {
                try {
                    val contentType = context.contentResolver.getType(selectedUri)
                    val ext = when {
                        contentType?.contains("png") == true -> ".png"
                        contentType?.contains("webp") == true -> ".webp"
                        else -> ".jpg"
                    }
                    val file = File(context.cacheDir, "crop_input_${UUID.randomUUID()}${ext}")
                    val copied = context.contentResolver.openInputStream(selectedUri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                        true
                    } ?: false

                    if (copied && file.length() > 0) {
                        tempFile = file
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (localUri != null) {
                // Remember the input file so we can delete it when the crop
                // activity returns (see cropLauncher callback above).
                pendingInputFile = tempFile
                val intent = Intent(context, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_SOURCE_URI, localUri)
                }
                cropLauncher.launch(intent)
            } else {
                viewModel.setError("无法读取图片，请重试")
            }
            pendingImageUri = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫描文本") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.mode == OcrMode.RESULT) {
                            viewModel.setMode(OcrMode.CHOOSE)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.mode == OcrMode.RESULT && uiState.recognizedText.isNotBlank()) {
                        TextButton(onClick = { onTextReady(uiState.editableText) }) {
                            Text("完成")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.mode) {
                OcrMode.CHOOSE -> ChooseModeContent(
                    onTakePhoto = {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onPickFromGallery = {
                        galleryLauncher.launch("image/*")
                    }
                )

                OcrMode.CAMERA -> {
                    // Fallback if camera preview isn't shown
                    ChooseModeContent(
                        onTakePhoto = {
                            if (hasCameraPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onPickFromGallery = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }

                OcrMode.RESULT -> ResultContent(
                    text = uiState.editableText,
                    onTextChange = { viewModel.updateEditableText(it) },
                    isProcessing = uiState.isProcessing,
                    error = uiState.error,
                    onRetry = {
                        capturedBitmap?.let { viewModel.processImage(it) }
                    },
                    onClearError = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun ChooseModeContent(
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "扫描英文文章",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "拍照或从相册选择包含英文文本的图片\n将自动识别其中的文字",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("拍照", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onPickFromGallery,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("从相册选择", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ResultContent(
    text: String,
    onTextChange: (String) -> Unit,
    isProcessing: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isProcessing) {
            LoadingOverlay(message = "正在识别文字...")
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error display
        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedButton(onClick = onRetry) {
                            Text("重试")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onClearError) {
                            Text("忽略")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Editable text
        Text(
            text = "识别结果（可编辑修正）",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("识别到的文字将显示在这里...") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Character count
        Text(
            text = "共 ${text.length} 个字符",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
