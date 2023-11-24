import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.rsahel.offlinemdb.DatabaseItem
import java.io.FileOutputStream
import java.io.IOException

class DatabaseHelper(private val mContext: Context) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        if (!checkDatabase())
            copyDatabase()
    }

    override fun onCreate(db: SQLiteDatabase) {
        // No need to create tables here if you are copying a pre-populated database
        Log.d(TAG, "onCreate()")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
        Log.d(TAG, "onUpgrade()")
    }

    // Method to copy the database from the assets folder to the application's data directory
    @Throws(IOException::class)
    fun copyDatabase() {
        val input = mContext.assets.open(DATABASE_NAME)
        val outputPath = mContext.getDatabasePath(DATABASE_NAME)

        outputPath.parentFile?.mkdirs() // Create necessary directories
        val output = FileOutputStream(outputPath)

        input.use { inputStream ->
            output.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    // Method to check if the database already exists
    private fun checkDatabase(): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            checkDB = openDatabase()
        } catch (e: Exception) {
            // Database does not exist yet
            Log.e(TAG, "checkDatabase: Database does not exist.")
        }
        checkDB?.close()
        return checkDB != null
    }

    // Method to open the database
    private fun openDatabase(): SQLiteDatabase {
        val databaseFile = mContext.getDatabasePath(DATABASE_NAME)
        return SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    fun getRowsWithTitle(query: String): MutableList<DatabaseItem> {
        val db = readableDatabase

        val selection = "$COLUMN_TITLE LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        val cursor = db.query(
            TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "numVotes DESC"
        )

        val result = mutableListOf<DatabaseItem>()

        // Sort the cursor based on Levenshtein distance
        cursor?.let { c ->
            if (c.moveToFirst()) {
                do {
                    result.add(DatabaseItem(c, c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE))))
                } while (c.moveToNext())
            }
        }


        return result
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length + 1
        val len2 = s2.length + 1

        val cost = Array(len1) { IntArray(len2) }

        for (i in 0 until len1) {
            cost[i][0] = i
        }

        for (j in 0 until len2) {
            cost[0][j] = j
        }

        for (i in 1 until len1) {
            for (j in 1 until len2) {
                cost[i][j] = minOf(
                    cost[i - 1][j] + 1,
                    cost[i][j - 1] + 1,
                    cost[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
            }
        }

        return cost[len1 - 1][len2 - 1]
    }


    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "im.db"
        private const val DATABASE_VERSION = 1

        private const val COLUMN_TITLE = "primaryTitle"
        private const val TABLE_NAME = "titles"
    }
}