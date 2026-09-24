// In file: di/DatabaseModule.kt
package com.example.kal.di

import android.content.Context
import androidx.room.Room
import com.example.kal.data.local.AppDatabase
import com.example.kal.data.local.DraftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This makes the database app-wide
object DatabaseModule {

    // This function tells Hilt how to create the AppDatabase.
    @Provides
    @Singleton // We only want one instance of the database.
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kal_database" // This is the file name of your database on the phone
        ).build()
    }

    // This function tells Hilt how to get the DraftDao.
    // Hilt already knows how to make AppDatabase (from the
    // function above), so this is easy.
    @Provides
    @Singleton
    fun provideDraftDao(db: AppDatabase): DraftDao {
        return db.draftDao()
    }
}