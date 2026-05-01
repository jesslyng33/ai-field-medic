package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "accessibility_needs",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AccessibilityNeeds(
    @PrimaryKey val userId: String,
    val visualImpairment: String,     // none / mild / severe / blind
    val hearingImpairment: String,    // none / mild / severe / deaf
    val mobilityLimitations: String   // none / limited / wheelchair / bedridden
)
