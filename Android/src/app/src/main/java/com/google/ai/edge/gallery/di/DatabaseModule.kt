package com.google.ai.edge.gallery.di

import android.content.Context
import androidx.room.Room
import com.google.ai.edge.gallery.database.AppDatabase
import com.google.ai.edge.gallery.database.dao.EmergencyEventDao
import com.google.ai.edge.gallery.database.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "field_medic_db").build()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideEmergencyEventDao(db: AppDatabase): EmergencyEventDao = db.emergencyEventDao()
}
