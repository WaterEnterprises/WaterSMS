package jv.watersms.enterprises.data

import kotlinx.coroutines.flow.Flow

class CampaignRepository(private val campaignDao: CampaignDao) {
    val allCampaigns: Flow<List<Campaign>> = campaignDao.getAllCampaignsFlow()

    fun getCampaignByIdFlow(id: Long): Flow<Campaign?> = campaignDao.getCampaignByIdFlow(id)

    suspend fun getCampaignById(id: Long): Campaign? = campaignDao.getCampaignById(id)

    fun getRecipientsForCampaignFlow(campaignId: Long): Flow<List<Recipient>> = 
        campaignDao.getRecipientsForCampaignFlow(campaignId)

    suspend fun getRecipientsForCampaign(campaignId: Long): List<Recipient> =
        campaignDao.getRecipientsForCampaign(campaignId)

    suspend fun getCampaignsByStatus(status: String): List<Campaign> =
        campaignDao.getCampaignsByStatus(status)

    suspend fun getPendingRecipients(campaignId: Long): List<Recipient> =
        campaignDao.getPendingRecipientsForCampaign(campaignId)

    suspend fun insertCampaign(campaign: Campaign): Long =
        campaignDao.insertCampaign(campaign)

    suspend fun insertRecipients(recipients: List<Recipient>) =
        campaignDao.insertRecipients(recipients)

    suspend fun updateCampaign(campaign: Campaign) =
        campaignDao.updateCampaign(campaign)

    suspend fun updateRecipient(recipient: Recipient) =
        campaignDao.updateRecipient(recipient)

    suspend fun updateCampaignStatus(campaignId: Long, status: String) =
        campaignDao.updateCampaignStatus(campaignId, status)

    suspend fun updateLastSentRecipientId(campaignId: Long, recipientId: Long) =
        campaignDao.updateLastSentRecipientId(campaignId, recipientId)

    suspend fun deleteCampaign(campaignId: Long) =
        campaignDao.deleteCampaign(campaignId)

    suspend fun getCampaignStats(campaignId: Long): CampaignStats {
        return CampaignStats(
            total = campaignDao.getTotalRecipientsCount(campaignId),
            sent = campaignDao.getSentRecipientsCount(campaignId),
            failed = campaignDao.getFailedRecipientsCount(campaignId)
        )
    }
}

data class CampaignStats(
    val total: Int,
    val sent: Int,
    val failed: Int
)
