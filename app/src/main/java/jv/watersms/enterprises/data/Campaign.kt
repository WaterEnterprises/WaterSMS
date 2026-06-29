package jv.watersms.enterprises.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class Campaign(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originalMessage: String,
    val variationsJson: String, // JSON array of 20 variations
    val minDelaySeconds: Int,
    val maxDelaySeconds: Int,
    val status: String, // "PENDING", "SENDING", "PAUSED", "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    /** ID of the last recipient processed. Used for crash recovery — on resume,
     *  skip recipients up to and including this ID to avoid duplicate sends. */
    val lastSentRecipientId: Long? = null
)
