package com.javis.ai.di

import android.content.Context
import androidx.room.Room
import com.javis.ai.memory.JavisDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJavisDatabase(@ApplicationContext context: Context): JavisDatabase {
        return Room.databaseBuilder(
            context,
            JavisDatabase::class.java,
            "javis_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideConversationDao(db: JavisDatabase) = db.conversationDao()

    @Provides
    fun provideUserMemoryDao(db: JavisDatabase) = db.userMemoryDao()

    @Provides
    fun provideAppUsageDao(db: JavisDatabase) = db.appUsageDao()
}
