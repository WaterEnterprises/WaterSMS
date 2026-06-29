package jv.watersms.enterprises.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipients",
    foreignKeys = [
        ForeignKey(
            entity = Campaign::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["campaignId"])]
)
data class Recipient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val name: String,
    val phoneNumber: String,
    val status: String, // "PENDING", "SENDING", "SENT", "FAILED"
    val sentMessage: String? = null,
    val errorMessage: String? = null,
    val sentAt: Long? = null
)
