package com.sysbot32.exifor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.sysbot32.exifor.databinding.ActivityMainBinding
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private lateinit var srcUri: Uri
    private lateinit var srcFilename: String
    private lateinit var srcExif: ExifInterface

    private val attributes = HashMap<String, String?>()

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

        binding.saveButton.setOnClickListener {
            if (!::srcUri.isInitialized) {
                return@setOnClickListener
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, srcFilename)
                put(MediaStore.Images.Media.MIME_TYPE, contentResolver.getType(srcUri))
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item = contentResolver.insert(collection, values)!!
            contentResolver.openFileDescriptor(item, "w", null).use {
                FileOutputStream(it?.fileDescriptor).use { outputStream ->
                    val inputStream = contentResolver.openInputStream(srcUri)!!
                    outputStream.write(inputStream.readBytes())
                    inputStream.close()
                    outputStream.close()
                }

            }

            val exif =
                ExifInterface(contentResolver.openFileDescriptor(item, "rw")!!.fileDescriptor)
            exif.setAttribute(ExifInterface.TAG_DATETIME, attributes[ExifInterface.TAG_DATETIME])
            exif.setAttribute(
                ExifInterface.TAG_DATETIME_DIGITIZED,
                attributes[ExifInterface.TAG_DATETIME]
            )
            exif.setAttribute(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                attributes[ExifInterface.TAG_DATETIME]
            )
            exif.saveAttributes()

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(item, values, null, null)

            Toast.makeText(this, R.string.image_saved_toast, Toast.LENGTH_SHORT).show()
        }

        binding.datetimeTextView.setOnClickListener {
            if (!::srcUri.isInitialized) {
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = String.format("%d:%02d:%02d", year, month + 1, dayOfMonth)
                    TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            val time = String.format("%02d:%02d:%02d", hourOfDay, minute, 0)
                            val datetime = "$date $time"
                            attributes[ExifInterface.TAG_DATETIME] = datetime
                            binding.datetimeTextView.text = datetime
                            srcFilename = String.format(
                                "%d%02d%02d_%02d%02d%02d.%s",
                                year,
                                month + 1,
                                dayOfMonth,
                                hourOfDay,
                                minute,
                                0,
                                srcFilename.split('.').last()
                            )
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.openButton.callOnClick()
        Toast.makeText(this, R.string.select_image_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                0 -> {
                    srcUri = data?.data!!
                    binding.imageView.setImageURI(srcUri)
                    contentResolver.query(srcUri, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        srcFilename = cursor.getString(nameIndex)
                    }
                    srcExif = ExifInterface(contentResolver.openInputStream(srcUri)!!)
                    if (srcExif.hasAttribute(ExifInterface.TAG_DATETIME)) {
                        val datetime = srcExif.getAttribute(ExifInterface.TAG_DATETIME)
                        attributes[ExifInterface.TAG_DATETIME] = datetime
                        binding.datetimeTextView.text = datetime
                    }
                    binding.saveButton.isEnabled = true
                }
            }
        }
    }
}