package com.shield.filequery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class MainActivity : AppCompatActivity() {

    private var globalList: MutableList<String> = mutableListOf()
    private var yourUri: Uri? = null
    private var requestStoragePermission: ActivityResultLauncher<String>? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                yourUri = result.data?.data
                handlePermissionResult(result.data?.data)

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isScopedStoragePermissionGranted()) {
            // Permission is already granted, you can now access the Downloads directory
            handlePermissionResult(yourUri)
        } else {
            // Request the permission if it is not granted
            requestScopedStoragePermission()
        }

    }

    private fun isScopedStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            return false
        }
    }

    private fun requestScopedStoragePermission() {
        // Use the Storage Access Framework to request permission
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        requestPermissionLauncher.launch(intent)
    }

    private fun handlePermissionResult(uri: Uri?) {
        if (uri != null) {
            val sharedPreferences = getPreferences(MODE_PRIVATE)
            sharedPreferences.edit().putString("grantedUri", uri.toString()).apply()

            globalList = getFileNamesFromUri(uri)
            if(globalList.isEmpty()){
                Toast.makeText(this, "Empty", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Not Empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where the URI is null (permission not granted)
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNamesFromUri(uri: Uri): MutableList<String> {
        val fileNames = mutableListOf<String>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        // Specify the file extensions you want to include
        val mimeTypeSelection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?)"
        val mimeTypeSelectionArgs = arrayOf("application/pdf", "text/plain", "application/zip")

        // Query the MediaStore for files, sorted by modification date in descending order (newest first)
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            mimeTypeSelection,
            mimeTypeSelectionArgs,
            sortOrder
        )

        cursor?.use {
            val displayNameColumnIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            while (it.moveToNext()) {
                val displayName = it.getString(displayNameColumnIndex)
                fileNames.add(displayName)
            }
        }

        return fileNames
    }
}

