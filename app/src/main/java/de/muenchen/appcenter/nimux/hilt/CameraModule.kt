package de.muenchen.appcenter.nimux.hilt

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import de.muenchen.appcenter.nimux.util.recognition.CameraController

/**
 * It is important that the CameraModule is part of the fragment lifecycle and no singleton.
 */
@Module
@InstallIn(FragmentComponent::class)
object CameraModule {

    @Provides
    fun provideCameraController(
        @ApplicationContext context: Context
    ): CameraController {
        return CameraController(context)
    }
}