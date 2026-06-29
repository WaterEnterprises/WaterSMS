package jv.watersms.enterprises.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import jv.watersms.enterprises.data.Campaign
import jv.watersms.enterprises.data.CampaignRepository
import jv.watersms.enterprises.data.GeminiRepository
import jv.watersms.enterprises.data.Recipient
import jv.watersms.enterprises.service.SmsSendingService
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface GeminiState {
    data object Idle : GeminiState
    data object Loading : GeminiState
    data class Success(val variations: List<String>) : GeminiState
    data class Error(val message: String) : GeminiState
}

@HiltViewModel
class SmsViewModel @Inject constructor(
    private val application: Application,
    private val repository: CampaignRepository,
) : androidx.lifecycle.ViewModel() {

    private val geminiRepository = GeminiRepository()

    val campaigns: StateFlow<List<Campaign>> = repository.allCampaigns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var campaignName = MutableStateFlow("")
    var originalMessage = MutableStateFlow("")
    var minDelay = MutableStateFlow("5")
    var maxDelay = MutableStateFlow("15")

    private val _geminiState = MutableStateFlow<GeminiState>(GeminiState.Idle)
    val geminiState: StateFlow<GeminiState> = _geminiState.asStateFlow()

    private val _deviceContacts = MutableStateFlow<List<ImportedContact>>(emptyList())
    val deviceContacts: StateFlow<List<ImportedContact>> = _deviceContacts.asStateFlow()

    private val _selectedContacts = MutableStateFlow<List<ImportedContact>>(emptyList())
    val selectedContacts: StateFlow<List<ImportedContact>> = _selectedContacts.asStateFlow()

    var manualContactsInput = MutableStateFlow("")

    private val prefs = application.getSharedPreferences("sms_prefs", android.content.Context.MODE_PRIVATE)
    var defaultRegion = MutableStateFlow(prefs.getString("default_region", "US") ?: "US")

    fun setDefaultRegion(region: String) {
        defaultRegion.value = region
        prefs.edit().putString("default_region", region).apply()
    }

    private val _selectedCampaignId = MutableStateFlow<Long?>(null)
    val selectedCampaignId: StateFlow<Long?> = _selectedCampaignId.asStateFlow()

    private val _selectedCampaign = MutableStateFlow<Campaign?>(null)
    val selectedCampaign: StateFlow<Campaign?> = _selectedCampaign.asStateFlow()

    private val _selectedCampaignRecipients = MutableStateFlow<List<Recipient>>(emptyList())
    val selectedCampaignRecipients: StateFlow<List<Recipient>> = _selectedCampaignRecipients.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedCampaignId.collect { id ->
                if (id != null) {
                    launch {
                        repository.getCampaignByIdFlow(id).collect { campaign ->
                            _selectedCampaign.value = campaign
                        }
                    }
                    launch {
                        repository.getRecipientsForCampaignFlow(id).collect { recipients ->
                            _selectedCampaignRecipients.value = recipients
                        }
                    }
                } else {
                    _selectedCampaign.value = null
                    _selectedCampaignRecipients.value = emptyList()
                }
            }
        }
    }

    fun selectCampaign(campaignId: Long?) {
        _selectedCampaignId.value = campaignId
    }

    fun getRecipientsFlow(campaignId: Long): kotlinx.coroutines.flow.Flow<List<Recipient>> {
        return repository.getRecipientsForCampaignFlow(campaignId)
    }

    fun loadDeviceContacts() {
        val region = defaultRegion.value
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = ContactImportHelper.fetchDeviceContacts(application, region)
            _deviceContacts.value = contacts
        }
    }

    fun toggleContactSelection(contact: ImportedContact) {
        val updatedDevice = _deviceContacts.value.map {
            if (it.phoneNumber == contact.phoneNumber) it.copy(isSelected = !it.isSelected) else it
        }
        _deviceContacts.value = updatedDevice
        _selectedContacts.value = updatedDevice.filter { it.isSelected }
    }

    fun selectAllContacts(select: Boolean) {
        val updatedDevice = _deviceContacts.value.map { it.copy(isSelected = select) }
        _deviceContacts.value = updatedDevice
        _selectedContacts.value = updatedDevice.filter { it.isSelected }
    }

    fun clearImportedContacts() {
        _deviceContacts.value = _deviceContacts.value.map { it.copy(isSelected = false) }
        _selectedContacts.value = emptyList()
        manualContactsInput.value = ""
    }

    fun generatePhrasings() {
        val message = originalMessage.value
        if (message.isBlank()) {
            _geminiState.value = GeminiState.Error("Original message cannot be empty")
            return
        }

        _geminiState.value = GeminiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val variations = geminiRepository.generateVariations(message)
                if (variations.isEmpty()) {
                    _geminiState.value = GeminiState.Error("No variations returned. Please try again.")
                } else {
                    _geminiState.value = GeminiState.Success(variations)
                }
            } catch (e: Exception) {
                _geminiState.value = GeminiState.Error(e.message ?: "Failed to generate variations")
            }
        }
    }

    fun createAndStartCampaign(onSuccess: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = campaignName.value.ifBlank { "Bulk Campaign" }
                val msg = originalMessage.value
                val minSec = minDelay.value.toIntOrNull() ?: 5
                val maxSec = maxDelay.value.toIntOrNull() ?: 15

                val variations = when (val state = _geminiState.value) {
                    is GeminiState.Success -> state.variations
                    else -> emptyList()
                }

                val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, String::class.java)
                val jsonAdapter = moshi.adapter<List<String>>(type)
                val variationsJson = jsonAdapter.toJson(variations)

                val campaign = Campaign(
                    name = name,
                    originalMessage = msg,
                    variationsJson = variationsJson,
                    minDelaySeconds = minSec,
                    maxDelaySeconds = maxSec,
                    status = "PENDING"
                )

                val campaignId = repository.insertCampaign(campaign)

                val contacts = mutableListOf<Recipient>()
                _selectedContacts.value.forEach {
                    contacts.add(Recipient(campaignId = campaignId, name = it.name, phoneNumber = it.phoneNumber, status = "PENDING"))
                }

                val manual = ContactImportHelper.parseManualContacts(manualContactsInput.value, defaultRegion.value)
                manual.forEach {
                    contacts.add(Recipient(campaignId = campaignId, name = it.name, phoneNumber = it.phoneNumber, status = "PENDING"))
                }

                if (contacts.isEmpty()) {
                    Log.e("SmsViewModel", "No contacts selected or imported")
                    return@launch
                }

                repository.insertRecipients(contacts)

                val context = application
                val intent = Intent(context, SmsSendingService::class.java).apply {
                    action = SmsSendingService.ACTION_START
                    putExtra(SmsSendingService.EXTRA_CAMPAIGN_ID, campaignId)
                }
                context.startService(intent)

                _selectedCampaignId.value = campaignId

                campaignName.value = ""
                originalMessage.value = ""
                _geminiState.value = GeminiState.Idle
                clearImportedContacts()

                withContext(Dispatchers.Main) {
                    onSuccess(campaignId)
                }
            } catch (e: Exception) {
                Log.e("SmsViewModel", "Failed to create campaign", e)
            }
        }
    }

    fun startCampaignSending(campaignId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val campaign = repository.getCampaignById(campaignId) ?: return@launch
            if (campaign.status != "SENDING") {
                val context = application
                val intent = Intent(context, SmsSendingService::class.java).apply {
                    action = SmsSendingService.ACTION_START
                    putExtra(SmsSendingService.EXTRA_CAMPAIGN_ID, campaignId)
                }
                context.startService(intent)
            }
        }
    }

    fun pauseCampaignSending(campaignId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = application
            val intent = Intent(context, SmsSendingService::class.java).apply {
                action = SmsSendingService.ACTION_PAUSE
                putExtra(SmsSendingService.EXTRA_CAMPAIGN_ID, campaignId)
            }
            context.startService(intent)
        }
    }

    fun deleteCampaign(campaignId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedCampaignId.value == campaignId) {
                val context = application
                val intent = Intent(context, SmsSendingService::class.java).apply {
                    action = SmsSendingService.ACTION_STOP
                }
                context.startService(intent)
                _selectedCampaignId.value = null
            }
            repository.deleteCampaign(campaignId)
        }
    }
}
