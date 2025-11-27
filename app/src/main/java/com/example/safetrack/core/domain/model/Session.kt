package com.example.safetrack.core.domain.model

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.UUID

enum class SessionType {
    @PropertyName("SOLO")
    SOLO,
    @PropertyName("GROUP")
    GROUP;

    companion object {
        @JvmStatic
        @PropertyName("type")
        fun fromString(value: String): SessionType {
            return valueOf(value.uppercase())
        }
    }
}

@Parcelize
data class Session(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: SessionType = SessionType.SOLO,
    val adminId: String = "",
    val adminEmail: String = "",
    val code: String = UUID.randomUUID().toString().substring(0, 6).uppercase(),
    val password: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val radiusLimit: Double? = null,
    val isActive: Boolean = true,
    val participants: List<Participant> = emptyList(),
    val adminName: String = "",
    val adminLocation: @RawValue GeoPoint? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val locationSharingEnabled: Boolean = true,
    val maxParticipants: Int? = null,
    val tags: List<String> = emptyList(),
    val settings: SessionSettings = SessionSettings()
) : Parcelable {
    fun isAdmin(userId: String): Boolean = adminId == userId
    
    fun getParticipantById(userId: String): Participant? =
        participants.find { it.id == userId }
    
    fun isParticipant(userId: String): Boolean =
        participants.any { it.id == userId }
    
    fun isActiveParticipant(userId: String): Boolean =
        participants.any { it.id == userId && it.status == ParticipantStatus.ACTIVE }
    
    fun hasLeftSession(userId: String): Boolean =
        participants.any { it.id == userId && it.status == ParticipantStatus.LEFT }
}

@Parcelize
data class SessionSettings(
    val allowParticipantChat: Boolean = true,
    val showParticipantLocations: Boolean = true,
    val notifyOnRadiusBreak: Boolean = true,
    val autoEndSession: Boolean = false,
    val autoEndDuration: Long? = null // Duration in milliseconds
) : Parcelable

@Parcelize
data class Participant(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val lastLocation: LatLng? = null,
    val lastUpdate: Long = System.currentTimeMillis(),
    val location: @RawValue GeoPoint? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val status: ParticipantStatus = ParticipantStatus.ACTIVE,
    val joinedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class ParticipantRole {
    ADMIN,
    MODERATOR,
    MEMBER
}

enum class ParticipantStatus {
    ACTIVE,
    INACTIVE,
    LEFT
}

@Parcelize
data class LatLng(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable