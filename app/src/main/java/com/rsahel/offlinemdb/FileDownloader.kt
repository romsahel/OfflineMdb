package com.rsahel.offlinemdb

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class FileDownloader {

    private val cacheFiles = mutableListOf<File>()

    fun downloadAndReadTSVGZFile(
        urlString: String,
        cacheFilename: String,
        context: Context,
        lineCallback: (String) -> Unit
    ) {
        val outputFile = File(context.cacheDir, cacheFilename)
        if (!outputFile.exists()) {
            downloadGZFile(urlString, outputFile)
        }
        cacheFiles.add(outputFile)
        decompressGzipFile(outputFile, lineCallback)
    }

    fun clearCacheFiles() {
        for (cacheFile in cacheFiles) {
            cacheFile.delete()
        }
    }

    fun downloadGZFile(urlString: String, outputFile: File) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var output: OutputStream? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return
            }

            inputStream = connection.inputStream
            output = FileOutputStream(outputFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                output.write(buffer, 0, bytesRead)
            }

            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
                output?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            connection?.disconnect()
        }
    }

    fun decompressGzipFile(file: File, lineCallback: (String) -> Unit) {
        var gzipInputStream: GZIPInputStream? = null
        var bufferedReader: BufferedReader? = null

        try {
            val fileInputStream = FileInputStream(file)
            gzipInputStream = GZIPInputStream(fileInputStream)
            bufferedReader = BufferedReader(InputStreamReader(gzipInputStream))

            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                lineCallback.invoke(line!!)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader?.close()
                gzipInputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
