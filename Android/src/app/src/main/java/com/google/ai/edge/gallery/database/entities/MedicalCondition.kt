package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_condition",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class MedicalCondition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val conditionName: String
)
