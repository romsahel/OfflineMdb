package com.rsahel.offlinemdb

import android.database.Cursor
import java.text.DecimalFormat

enum class ItemType {
    short,
    movie,
    tvShort,
    tvSeries,
    tvMovie,
    tvEpisode,
    tvMiniSeries,
    tvSpecial,
    video,
    videoGame,
}

class DatabaseItem {
    val id: String;
    val title: String;
    val rating: Float;
    val numVotes: Int;
    val year: String;
    val genres: String;
    val runtime: String;
    val type: ItemType;

    constructor(c: Cursor, title: String) {
        this.title = title
        this.id = c.getString(c.getColumnIndexOrThrow("tconst"))
        this.rating = c.getFloat(c.getColumnIndexOrThrow("averageRating"))
        this.numVotes = c.getInt(c.getColumnIndexOrThrow("numVotes"))
        this.year = c.getString(c.getColumnIndexOrThrow("startYear"))
        this.genres = c.getString(c.getColumnIndexOrThrow("genres"))
        this.runtime = c.getString(c.getColumnIndexOrThrow("runtimeMinutes"))
        this.type = ItemType.valueOf(c.getString(c.getColumnIndexOrThrow("titleType")))
    }

    constructor(
        id: String,
        title: String,
        rating: Float,
        numVotes: Int,
        year: String,
        genres: String,
        runtime: String,
        type: ItemType
    ) {
        this.id = id
        this.title = title
        this.rating = rating
        this.numVotes = numVotes
        this.year = year
        this.genres = genres
        this.runtime = runtime
        this.type = type
    }

    fun formattedRuntime(): String {
        if (this.runtime == "\\N") return this.runtime;

        val runtime = this.runtime.toInt();
        val hours = runtime / 60;
        val minutes = runtime - hours * 60;
        return "${hours}h${"$minutes".padStart(1)}m";
    }

    fun formattedType(): String {
        val type = "${this.type}"
        if (type[0] == 't' && type[1] == 'v')
            return "TV ${type.substring(2)}"
        return type.replaceFirstChar(Char::titlecase)
    }

    fun formattedNumVotes(): String {
        val df = DecimalFormat("#.##")
        if (numVotes > 1e6) {
            return "${df.format(numVotes / 1e6f)}M"
        }
        if (numVotes > 1000) {
            return "${df.format(numVotes / 1000.0f)}k"
        }
        return "$numVotes"
    }
}