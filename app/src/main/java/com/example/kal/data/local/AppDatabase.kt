// In file: data/local/AppDatabase.kt
package com.example.kal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// This annotation tells Room to build a database.
// List all your entities (tables) in the array.
@Database(
    entities = [Draft::class],
    version = 1 // Increment this if you change the schema (table structure)
)
abstract class AppDatabase : RoomDatabase() {

    // This abstract function connects the database to the DAO.
    // Room will auto-generate the code for this.
    abstract fun draftDao(): DraftDao

}