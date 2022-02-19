package com.delprks.wave.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.delprks.wave.App.Companion.applicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageCache {

    private fun saveImgToCache(bitmap: Bitmap, name: String?): File? {
        var cachePath: File? = null
        var fileName: String? = TEMP_FILE_NAME
        if (!TextUtils.isEmpty(name)) {
            fileName = name
        }
        try {
            cachePath = File(applicationContext().cacheDir, CHILD_DIR)
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/$fileName$FILE_EXTENSION")
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, stream)
            stream.close()
        } catch (e: IOException) {
            Log.e(TAG, "saveImgToCache error: $bitmap", e)
        }

        return cachePath
    }

    @JvmOverloads
    fun saveToCacheAndGetUri(bitmap: Bitmap, name: String? = null): Uri {
        val file = saveImgToCache(bitmap, name)
        return getImageUri(file, name)
    }

    fun getUriByFileName(name: String): Uri? {
        val context: Context = applicationContext()
        val fileName: String = if (!TextUtils.isEmpty(name)) {
            name
        } else {
            return null
        }
        val imagePath = File(context.cacheDir, CHILD_DIR)
        val newFile = File(imagePath, fileName + FILE_EXTENSION)
        return FileProvider.getUriForFile(context, context.packageName + ".provider", newFile)
    }

    // Get an image Uri by name without extension from a file dir
    private fun getImageUri(fileDir: File?, name: String?): Uri {
        val context: Context = applicationContext()
        var fileName: String? = TEMP_FILE_NAME
        if (!TextUtils.isEmpty(name)) {
            fileName = name
        }
        val newFile = File(fileDir, fileName + FILE_EXTENSION)
        return FileProvider.getUriForFile(context, context.packageName + ".provider", newFile)
    }

    companion object {
        val TAG: String = ImageCache::class.java.simpleName

        private const val CHILD_DIR = "images"
        const val FILE_EXTENSION = ".jpg"
        private const val TEMP_FILE_NAME = "img"
        private const val COMPRESS_QUALITY = 50

        fun getPath(name: String): String {
            return "${applicationContext().cacheDir}/$CHILD_DIR/$name$FILE_EXTENSION"
        }

        fun getName(name: String): String {
            return "$name$FILE_EXTENSION"
        }

        fun getUri(name: String): Uri {
            return Uri.parse("${applicationContext().cacheDir}/$CHILD_DIR/$name")
        }
    }
}