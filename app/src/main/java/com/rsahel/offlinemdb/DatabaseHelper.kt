package com.rsahel.offlinemdb

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.util.Range
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DatabaseHelper(mContext: Context, val onUpdated: (DatabaseHelper) -> Unit) :
    SQLiteOpenHelper(mContext, "im.db", null, DATABASE_VERSION) {

    var tableExists: Boolean = false

    init {
        instance = this
        updateIfTableExists()
        onUpdated(this)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate()")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade()")
    }

    private fun setLastUpdate(context: Context) {
        val sharedPref = context.getSharedPreferences(TAG, MODE_PRIVATE);

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        sharedPref.edit().putString("LastUpdate", current.format(formatter)).apply();
    }

    fun getLastUpdate(context: Context): String? {
        val sharedPref = context.getSharedPreferences(TAG, MODE_PRIVATE);
        return sharedPref.getString("LastUpdate", null);
    }

    private fun setItemCount(context: Context, itemCount: Int) {
        val sharedPref = context.getSharedPreferences(TAG, MODE_PRIVATE);
        sharedPref.edit().putInt("ItemCount", itemCount).apply();
    }

    fun getItemCount(context: Context): Int {
        val sharedPref = context.getSharedPreferences(TAG, MODE_PRIVATE);
        return sharedPref.getInt("ItemCount", 0);
    }

    fun updateDatabase(context: Context, progressCallback: ((Int) -> Unit)) {
        val db = writableDatabase

        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME_NEW;")
        db.execSQL("CREATE TABLE $TABLE_NAME_NEW ($ID_NAME TEXT PRIMARY KEY);")

        val set = HashSet<String>()
        val downloader = FileDownloader()
        val itemCount = downloadAndReadTitleRatings(downloader, db, context, set, progressCallback)
        downloadAndReadTitleBasics(downloader, db, context, set, progressCallback)

        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME_CURRENT;")
        db.execSQL("ALTER TABLE $TABLE_NAME_NEW RENAME TO $TABLE_NAME_CURRENT;")

        db.close()

        downloader.clearCacheFiles()
        setLastUpdate(context)
        setItemCount(context, itemCount)
        updateIfTableExists()
        onUpdated(this)
    }

    private fun downloadAndReadTitleRatings(
        downloader: FileDownloader,
        db: SQLiteDatabase,
        context: Context,
        set: HashSet<String>,
        progressCallback: (Int) -> Unit,
    ): Int {
        fun addColumns(columnNames: List<String>) {
            db.execSQL("ALTER TABLE $TABLE_NAME_NEW ADD COLUMN ${columnNames[1]} REAL;")
            db.execSQL("ALTER TABLE $TABLE_NAME_NEW ADD COLUMN ${columnNames[2]} INT;")
        }

        fun isRowValid(values: List<String>): Boolean {
            return values[2].toInt() > 150
        }

        fun insertRow(columnNames: List<String>, values: List<String>) {
            val contentValues = ContentValues()
            contentValues.put(columnNames[0], values[0])
            contentValues.put(columnNames[1], values[1].toFloat())
            contentValues.put(columnNames[2], values[2].toInt())
            set.add(values[0])
            db.insert(TABLE_NAME_NEW, null, contentValues)
        }

        return downloadAndProcess(
            downloader,
            db,
            context,
            "https://datasets.imdbws.com/title.ratings.tsv.gz",
            "title.ratings.tsv.gz",
            Range(0, 25),
            progressCallback,
            ::addColumns,
            ::isRowValid,
            ::insertRow,
        )
    }

    private fun downloadAndReadTitleBasics(
        downloader: FileDownloader,
        db: SQLiteDatabase,
        context: Context,
        set: HashSet<String>,
        progressCallback: (Int) -> Unit
    ) {
        fun isColumnAllowed(columnName: String): Boolean {
            return (columnName != "isAdult"
                    && columnName != "endYear"
                    )
        }

        var primaryTitleIndex = -1
        var originalTitleIndex = -1
        val columnIndices = mutableListOf<Int>()
        fun addColumns(columnNames: List<String>) {
            for ((i, columnName) in columnNames.withIndex()) {
                if (i != 0 && isColumnAllowed(columnName)) {
                    Log.d("DatabaseHelper", "Add column ${columnName}")
                    db.execSQL("ALTER TABLE $TABLE_NAME_NEW ADD COLUMN ${columnName} TEXT;")
                    columnIndices.add(i)
                    if (columnName == COLUMN_TITLE) {
                        primaryTitleIndex = i
                    } else if (columnName == COLUMN_ORIGINAL_TITLE) {
                        originalTitleIndex = i
                    }
                }
            }
        }

        fun isRowValid(values: List<String>): Boolean {
            return set.contains(values[0])
        }

        fun insertRow(columnNames: List<String>, values: List<String>) {
            val contentValues = ContentValues()
            for (i in columnIndices) {
                contentValues.put(columnNames[i], values[i])
            }
            if (values[primaryTitleIndex] == values[originalTitleIndex]) {
                contentValues.remove(columnNames[originalTitleIndex])
            }

            val whereClause = "$ID_NAME = ?"
            val whereArgs = arrayOf(values[0])
            db.update(TABLE_NAME_NEW, contentValues, whereClause, whereArgs)
        }

        downloadAndProcess(
            downloader,
            db,
            context,
            "https://datasets.imdbws.com/title.basics.tsv.gz",
            "title.basics.tsv.gz",
            Range(25, 100),
            progressCallback,
            ::addColumns,
            ::isRowValid,
            ::insertRow,
        )
    }

    private fun downloadAndProcess(
        downloader: FileDownloader,
        db: SQLiteDatabase,
        context: Context,
        urlString: String,
        cacheFilename: String,
        progressRange: Range<Int>,
        progressCallback: (Int) -> Unit,
        addColumns: (List<String>) -> Unit,
        isRowValid: (List<String>) -> Boolean,
        insertOrUpdateRow: (List<String>, List<String>) -> Unit,
    ): Int {
        Log.d(TAG, "Start download process for $cacheFilename")
        val maxCounter = 300000
        var counter = 0
        var columnNames: List<String>? = null
        var previousPercentage = 0
        downloader.downloadAndReadTSVGZFile(
            urlString,
            cacheFilename,
            context,
        ) { line ->
            val values = line.split("\t")
            if (columnNames == null) {
                columnNames = values
                addColumns(columnNames!!)
                db.beginTransaction()
            } else if (isRowValid(values)) {
                insertOrUpdateRow(columnNames!!, values)
                val percentage =
                    (progressRange.lower + (progressRange.upper - progressRange.lower) * (counter++ / maxCounter.toFloat())).toInt()
                if (previousPercentage != percentage) {
                    previousPercentage = percentage
                    progressCallback(percentage)
                }
            }
        }

        db.setTransactionSuccessful()
        db.endTransaction()
        Log.d(TAG, "End download process for $cacheFilename ($counter items)")
        return counter
    }

    fun getItemsWithTitle(query: String, max: Int = 50): MutableList<DatabaseItem> {
        val db = readableDatabase

        val result = mutableListOf<DatabaseItem>()

        if (!tableExists) {
            return result
        }

        val selection = "$COLUMN_TITLE LIKE ? OR $COLUMN_ORIGINAL_TITLE LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%")
        val orderBy = "numVotes DESC"
        val cursor = db.query(
                TABLE_NAME_CURRENT,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
            )

        cursor?.let { c ->
            if (c.moveToFirst()) {
                do {
                    result.add(DatabaseItem(c, c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE))))
                } while (c.moveToNext() && result.size < max)
            }
        }


        return result
    }

    private fun updateIfTableExists() {
        val db = readableDatabase
        var cursor: Cursor? = null

        tableExists = false
        try {
            cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(TABLE_NAME_CURRENT)
            )
            Log.d(TAG, "Table contains ${cursor?.count} elements")
            tableExists = (cursor?.count ?: 0) > 0
        } finally {
            cursor?.close()
            db.close()
        }


    }

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_VERSION = 1

        private const val COLUMN_TITLE = "primaryTitle"
        private const val COLUMN_ORIGINAL_TITLE = "originalTitle"
        private const val TABLE_NAME_CURRENT = "titles"
        private const val TABLE_NAME_NEW = "titles_new"
        private const val ID_NAME = "tconst"

        private var instance: DatabaseHelper? = null
        fun getInstance(): DatabaseHelper? {
            return instance
        }
    }
}