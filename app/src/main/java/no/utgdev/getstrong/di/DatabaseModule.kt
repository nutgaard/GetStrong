package no.utgdev.getstrong.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import no.utgdev.getstrong.data.local.dao.ExerciseDao
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.WorkoutDao
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.db.GetStrongDatabase
import no.utgdev.getstrong.data.local.db.MIGRATION_1_2

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GetStrongDatabase =
        Room.databaseBuilder(
            context,
            GetStrongDatabase::class.java,
            "getstrong.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideExerciseDao(database: GetStrongDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutDao(database: GetStrongDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideSessionDao(database: GetStrongDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideWorkoutSummaryDao(database: GetStrongDatabase): WorkoutSummaryDao =
        database.workoutSummaryDao()
}
