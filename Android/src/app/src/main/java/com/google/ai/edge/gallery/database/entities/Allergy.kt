package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "allergy",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Allergy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val allergen: String,
    val severity: String,  // mild / moderate / severe / life-threatening
    val reaction: String   // anaphylaxis / rash / hives / swelling / etc.
)
