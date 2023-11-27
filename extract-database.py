import urllib.request
import gzip
import pandas as pd
import sqlite3


def download_and_unzip(filename):
    print(f"Downloading {filename}...")
    urllib.request.urlretrieve(f"https://datasets.imdbws.com/{filename}", filename)
    print(f"Unzipping {filename}...")
    with gzip.open(filename) as f:
        data = pd.read_csv(f, sep="\t")
    return data


id = "tconst"

basics = download_and_unzip("title.basics.tsv.gz")
ratings = download_and_unzip("title.ratings.tsv.gz")

# Check if the id column exists in both DataFrames
if id not in basics.columns or id not in ratings.columns:
    raise ValueError(f"Both DataFrames must have a column named {id} for merging.")

# Check if the data types of the id column match in both DataFrames
if basics[id].dtype != ratings[id].dtype:
    raise ValueError("Data type mismatch for the id column in the two DataFrames.")

# Connect to the SQLite database
db_connection = sqlite3.connect("im2.db")

merged_df = basics # pd.merge(basics, ratings, on=id)

# Save the merged DataFrame to the SQLite database, excluding certain columns
columns_to_exclude = [
    "originalTitle",
    "isAdult",
    "endYear",
]
merged_df = merged_df.drop(columns=columns_to_exclude, errors="ignore")

valid_title_types = [
    "short",
    "movie",
    "tvShort",
    "tvSeries",
    "tvMovie",
    # "tvEpisode",
    "tvMiniSeries",
    "tvSpecial",
    # "video",
    # "videoGame",
]
merged_df = merged_df[merged_df["titleType"].isin(valid_title_types)]
merged_df.to_sql("titles", db_connection, index=False, if_exists="replace")

# Query the entire table
# query = "SELECT * FROM titles"
# result_df = pd.read_sql(query, db_connection)

# Print the content of the DataFrame
# print(result_df)

# Close the database connection
db_connection.close()
