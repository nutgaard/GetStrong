package no.utgdev.getstrong.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import no.utgdev.getstrong.data.repository.ExerciseRepositoryImpl
import no.utgdev.getstrong.data.repository.SessionRepositoryImpl
import no.utgdev.getstrong.data.repository.SettingsRepositoryImpl
import no.utgdev.getstrong.data.repository.WorkoutRepositoryImpl
import no.utgdev.getstrong.data.repository.WorkoutSummaryRepositoryImpl
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutSummaryRepository(impl: WorkoutSummaryRepositoryImpl): WorkoutSummaryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
