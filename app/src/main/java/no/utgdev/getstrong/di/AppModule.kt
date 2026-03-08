package no.utgdev.getstrong.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import no.utgdev.getstrong.data.time.SystemTimeProvider
import no.utgdev.getstrong.domain.time.TimeProvider
import no.utgdev.getstrong.ui.activeWorkout.RestSignalPlayer
import no.utgdev.getstrong.ui.activeWorkout.ToneRestSignalPlayer

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindRestSignalPlayer(impl: ToneRestSignalPlayer): RestSignalPlayer
}
