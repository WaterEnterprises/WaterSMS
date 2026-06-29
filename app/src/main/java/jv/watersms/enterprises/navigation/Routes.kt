package jv.watersms.enterprises.navigation

sealed class Route(val route: String) {
    data object Launch : Route("launch")
    data object Campaigns : Route("campaigns")
    data object CampaignDetail : Route("campaign_detail/{campaignId}") {
        fun createRoute(campaignId: Long) = "campaign_detail/$campaignId"
    }
    data object Settings : Route("settings")
}
