package com.paopao.englearn.ui.ocr

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageView
import com.paopao.englearn.R
import java.io.File
import java.io.FileOutputStream

class CropActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"
    }

    private lateinit var cropImageView: CropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crop)

        cropImageView = findViewById(R.id.cropImageView)
        cropImageView.setFixedAspectRatio(false)

        @Suppress("DEPRECATION")
        val sourceUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SOURCE_URI, Uri::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_SOURCE_URI)
        }

        if (sourceUri != null) {
            cropImageView.setImageUriAsync(sourceUri)
        } else {
            Toast.makeText(this, R.string.error_image_load, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cropImageView.setOnCropImageCompleteListener { _, cropResult ->
            val bitmap = cropResult.bitmap
            if (bitmap != null) {
                val outFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                val resultUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    outFile
                )
                intent.putExtra(EXTRA_RESULT_URI, resultUri)
                setResult(RESULT_OK, intent)
            }
            finish()
        }

        findViewById<android.widget.Button>(R.id.cropConfirmButton).setOnClickListener {
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
