package no.utgdev.getstrong.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.utgdev.getstrong.data.local.dao.ExerciseDao
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.WorkoutDao
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.db.GetStrongDatabase
import no.utgdev.getstrong.data.local.db.MIGRATION_1_2
import no.utgdev.getstrong.data.local.db.MIGRATION_2_3
import no.utgdev.getstrong.data.local.db.MIGRATION_3_4
import no.utgdev.getstrong.data.local.db.MIGRATION_4_5
import no.utgdev.getstrong.data.local.db.MIGRATION_5_6
import no.utgdev.getstrong.data.seed.ExerciseSeedData

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GetStrongDatabase {
        val database = Room.databaseBuilder(
            context,
            GetStrongDatabase::class.java,
            "getstrong.db",
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .build()
        runBlocking {
            database.withTransaction {
                database.exerciseDao().insertAllIgnore(ExerciseSeedData.exercises)
            }
        }
        return database
    }

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
