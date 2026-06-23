package com.paopao.englearn.ui.ocr

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.canhub.cropper.CropImageView
import com.paopao.englearn.R
import java.io.File
import java.io.FileOutputStream

class CropActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        // Absolute path of the cropped output file, so the caller can decode it
        // and delete it afterward — temp files in cacheDir otherwise accumulate.
        const val EXTRA_RESULT_PATH = "extra_result_path"
    }

    private lateinit var cropImageView: CropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceUri = IntentCompat.getParcelableExtra(
            intent, EXTRA_SOURCE_URI, Uri::class.java
        )
        if (sourceUri == null) {
            // Nothing to crop — bail out so the caller can recover.
            Toast.makeText(this, R.string.error_image_load, Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_crop)

        cropImageView = findViewById(R.id.cropImageView)
        cropImageView.setFixedAspectRatio(false)
        cropImageView.setImageUriAsync(sourceUri)

        cropImageView.setOnCropImageCompleteListener { _, cropResult ->
            val bitmap = cropResult.bitmap
            if (bitmap != null) {
                try {
                    val outFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    }
                    intent.putExtra(EXTRA_RESULT_PATH, outFile.absolutePath)
                    setResult(RESULT_OK, intent)
                } catch (_: Exception) {
                    setResult(RESULT_CANCELED)
                }
            } else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }

        findViewById<android.widget.Button>(R.id.cropConfirmButton).setOnClickListener { button ->
            // Disable immediately so a double-tap can't kick off two crops.
            button.isEnabled = false
            cropImageView.croppedImageAsync(
                Bitmap.CompressFormat.JPEG,
                90,
                0, 0,
                CropImageView.RequestSizeOptions.RESIZE_FIT,
                null
            )
        }
    }
}
