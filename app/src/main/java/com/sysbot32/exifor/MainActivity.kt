package com.sysbot32.exifor

import android.content.Intent
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.sysbot32.exifor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openButton.setOnClickListener {
            startActivityForResult(
                Intent().setAction(Intent.ACTION_PICK)
                    .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*"), 0
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                0 -> {
                    val uri = data?.data!!
                    binding.imageView.setImageURI(uri)
                    val exif = ExifInterface(contentResolver.openInputStream(uri)!!)
                    if (exif.hasAttribute(ExifInterface.TAG_DATETIME)) {
                        binding.datetimeTextView.text =
                            exif.getAttribute(ExifInterface.TAG_DATETIME)
                    }
                }
            }
        }
    }
}