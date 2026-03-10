package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.planningFabInset(): Modifier =
    this
        .navigationBarsPadding()
        .padding(end = 16.dp, bottom = 16.dp)
