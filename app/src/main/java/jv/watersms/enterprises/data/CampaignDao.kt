package jv.watersms.enterprises.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaignsFlow(): Flow<List<Campaign>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): Campaign?

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun getCampaignByIdFlow(id: Long): Flow<Campaign?>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId")
    fun getRecipientsForCampaignFlow(campaignId: Long): Flow<List<Recipient>>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId")
    suspend fun getRecipientsForCampaign(campaignId: Long): List<Recipient>

    @Query("SELECT * FROM campaigns WHERE status = :status")
    suspend fun getCampaignsByStatus(status: String): List<Campaign>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId AND status = 'PENDING'")
    suspend fun getPendingRecipientsForCampaign(campaignId: Long): List<Recipient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: Campaign): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipients(recipients: List<Recipient>)

    @Update
    suspend fun updateCampaign(campaign: Campaign)

    @Update
    suspend fun updateRecipient(recipient: Recipient)

    @Query("UPDATE campaigns SET status = :status WHERE id = :campaignId")
    suspend fun updateCampaignStatus(campaignId: Long, status: String)

    @Query("UPDATE campaigns SET lastSentRecipientId = :recipientId WHERE id = :campaignId")
    suspend fun updateLastSentRecipientId(campaignId: Long, recipientId: Long)

    @Query("DELETE FROM campaigns WHERE id = :campaignId")
    suspend fun deleteCampaign(campaignId: Long)

    @Query("SELECT COUNT(*) FROM recipients WHERE campaignId = :campaignId")
    suspend fun getTotalRecipientsCount(campaignId: Long): Int

    @Query("SELECT COUNT(*) FROM recipients WHERE campaignId = :campaignId AND status = 'SENT'")
    suspend fun getSentRecipientsCount(campaignId: Long): Int

    @Query("SELECT COUNT(*) FROM recipients WHERE campaignId = :campaignId AND status = 'FAILED'")
    suspend fun getFailedRecipientsCount(campaignId: Long): Int
}
