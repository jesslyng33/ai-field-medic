package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class UserContextViewModel @Inject constructor(
    private val repo: UserProfileRepository,
) : ViewModel() {

    fun loadContext(
        tripLocation: String,
        soloTraveler: Boolean,
        firstAidKit: Set<String>,
        onReady: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repo.profileWithDetails.first()
            if (profile != null) {
                val ctx = buildUserMedicalContext(
                    profile = profile,
                    tripLocation = tripLocation,
                    soloTraveler = soloTraveler,
                    firstAidKit = firstAidKit,
                )
                AssessmentData.userContext = ctx
            }
            AssessmentData.tripLocation = tripLocation
            AssessmentData.soloTraveler = soloTraveler
            AssessmentData.firstAidKit = firstAidKit
            withContext(Dispatchers.Main) { onReady() }
        }
    }
}
