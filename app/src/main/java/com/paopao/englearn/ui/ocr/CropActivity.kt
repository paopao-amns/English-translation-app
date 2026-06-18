package com.paopao.englearn.ui.ocr

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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

        val sourceUri = intent.getParcelableExtra(EXTRA_SOURCE_URI, Uri::class.java)
        if (sourceUri != null) {
            cropImageView.setImageUriAsync(sourceUri)
        }

        cropImageView.setOnCropImageCompleteListener { _, cropResult ->
            val bitmap = cropResult.bitmap
            if (bitmap != null) {
                val outFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                val resultUri = Uri.fromFile(outFile)
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
