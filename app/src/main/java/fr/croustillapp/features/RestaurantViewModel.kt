package fr.croustillapp.features

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.croustillapp.R
import fr.croustillapp.data.AppDatabase
import fr.croustillapp.data.DailyMenuDto
import fr.croustillapp.data.FavoriteManager
import fr.croustillapp.data.Restaurant
import fr.croustillapp.data.StatusUpdatePartial
import fr.croustillapp.data.toDomain
import fr.croustillapp.data.toEntity
import fr.croustillapp.network.NetworkMonitor
import fr.croustillapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("croustillapp_prefs", Context.MODE_PRIVATE)
    private val lastUpdateKey = "last_status_update_timestamp"

    private val database = AppDatabase.getDatabase(application)
    private val restaurantDao = database.restaurantDao()
    private val apiService = RetrofitClient.getService(application)

    private val favoriteManager = FavoriteManager(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _errorEvents = MutableSharedFlow<Int>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _searchText = MutableStateFlow("")
    private val _selectedRegion = MutableStateFlow("Toutes")
    private val _selectedType = MutableStateFlow("Tous")
    private val _showOnlyOpen = MutableStateFlow(false)
    private val _showOnlyPmr = MutableStateFlow(false)

    val searchText = _searchText.asStateFlow()
    val selectedRegion = _selectedRegion.asStateFlow()
    val selectedType = _selectedType.asStateFlow()
    val showOnlyOpen = _showOnlyOpen.asStateFlow()
    val showOnlyPmr = _showOnlyPmr.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoriteManager.favoriteIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var isCurrentlyLoading = false

    private val _isError = MutableStateFlow(false)
    val isError = _isError.asStateFlow()

    private val _menuState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val menuState: StateFlow<MenuUiState> = _menuState.asStateFlow()

    private var lastLoadedRestaurantId: String? = null
    private var menuJob: Job? = null

    sealed class MenuUiState {
        object Idle : MenuUiState()
        object Loading : MenuUiState()
        data class Success(val data: List<DailyMenuDto>) : MenuUiState()
        object Error : MenuUiState()
    }

    fun loadMenu(restaurantId: String) {
        if (lastLoadedRestaurantId == restaurantId && _menuState.value is MenuUiState.Success) return

        menuJob?.cancel()
        menuJob = viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            lastLoadedRestaurantId = restaurantId

            try {
                delay(1000)
                val response = apiService.getMenu(restaurantId)
                if (response.success) {
                    _menuState.value = MenuUiState.Success(response.data)
                } else {
                    _menuState.value = MenuUiState.Success(emptyList())
                }
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 404) {
                    _menuState.value = MenuUiState.Success(emptyList())
                } else {
                    _menuState.value = MenuUiState.Error
                }
            }
        }
    }

    private val allRestaurants: StateFlow<List<Restaurant>> = restaurantDao.getAllRestaurants()
        .distinctUntilChanged()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val typesList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Tous") + list.map { it.type }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tous"))

    val regionsList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Toutes") + list.map { it.region }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Toutes"))

    @OptIn(FlowPreview::class)
    private val debouncedSearchText = _searchText
        .debounce(333)
        .distinctUntilChanged()

    private val filterParams = combine(
        debouncedSearchText,
        _selectedRegion,
        _selectedType,
        _showOnlyOpen,
        _showOnlyPmr
    ) { query, region, type, onlyOpen, onlyPmr ->
        FilterUiParams(query, region, type, onlyOpen, onlyPmr)
    }

    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        allRestaurants,
        filterParams
    ) { list, params ->
        list.filter { resto ->
            val matchesSearch = params.query.isEmpty() ||
                    resto.name.contains(params.query, ignoreCase = true) ||
                    resto.id.contains(params.query)

            val matchesRegion = params.region == "Toutes" || resto.region == params.region
            val matchesType = params.type == "Tous" || resto.type == params.type
            val matchesOpen = !params.onlyOpen || resto.isOpen
            val matchesPmr = !params.onlyPmr || resto.pmr

            matchesSearch && matchesRegion && matchesType && matchesOpen && matchesPmr
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .collect { online ->
                    if (online) {
                        checkAndLoadData()
                    }
                }
        }
    }

    private fun checkAndLoadData() {
        if (isCurrentlyLoading) return

        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                restaurantDao.getRestaurantsCount()
            }

            if (count == 0) {
                loadData(forceRefresh = true)
            } else {
                loadData(forceRefresh = false)
            }
        }
    }

    fun updateSearchText(query: String) { _searchText.value = query }
    fun updateRegion(region: String) { _selectedRegion.value = region }
    fun updateType(type: String) { _selectedType.value = type }
    fun toggleOnlyOpen(show: Boolean) { _showOnlyOpen.value = show }
    fun toggleOnlyPmr(show: Boolean) { _showOnlyPmr.value = show }

    private suspend fun loadData(forceRefresh: Boolean) {
        if (isCurrentlyLoading) return
        isCurrentlyLoading = true

        try {
            if (forceRefresh) {
                _isLoading.value = true
                val fullResponse = apiService.getRestaurants()
                if (fullResponse.success) {
                    withContext(Dispatchers.IO) {
                        restaurantDao.insertAll(fullResponse.data.map { it.toEntity() })
                    }
                }
            }

            refreshOnlyStatuses()

        } catch (_: Exception) {
            _isError.value = true
        } finally {
            _isLoading.value = false
            isCurrentlyLoading = false
        }
    }

    private suspend fun refreshOnlyStatuses() {
        val currentTime = System.currentTimeMillis()
        val lastUpdate = sharedPreferences.getLong(lastUpdateKey, 0L)
        val fiveMinutesInMs = 5 * 60 * 1000 // 300 000 ms

        if (currentTime - lastUpdate < fiveMinutesInMs) {
            return
        }

        try {
            val statusResponse = apiService.getRestaurantsStatus()

            if (statusResponse.success) {
                val updates = statusResponse.data.map { dto ->
                    StatusUpdatePartial(id = dto.code.toString(), isOpen = dto.ouvert)
                }
                withContext(Dispatchers.IO) {
                    restaurantDao.updateAllStatuses(updates)
                }

                sharedPreferences.edit { putLong(lastUpdateKey, currentTime) }
            }
        } catch (_: Exception) {

        }
    }

    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch {
            val currentFavorites = favoriteIds.value
            val isAlreadyFavorite = currentFavorites.contains(restaurantId)

            if (!isAlreadyFavorite) {
                if (currentFavorites.size >= 6) {
                    _errorEvents.emit(R.string.toast_favoris)
                } else {
                    favoriteManager.saveFavorites(currentFavorites + restaurantId)
                }
            } else {
                favoriteManager.saveFavorites(currentFavorites - restaurantId)
            }
        }
    }

    private val _deepLinkRestaurant = MutableStateFlow<Restaurant?>(null)
    val deepLinkRestaurant = _deepLinkRestaurant.asStateFlow()

    fun clearDeepLinkRestaurant() {
        _deepLinkRestaurant.value = null
    }

    fun loadSingleRestaurantFromDeepLink(restaurantId: String?) {
        viewModelScope.launch {
            if (restaurantId.isNullOrEmpty()) {
                _errorEvents.emit(R.string.error_deeplink_invalid)
                return@launch
            }

            val isOnlineNow = networkMonitor.isOnline.first()
            if (!isOnlineNow) {
                _errorEvents.emit(R.string.error_deeplink_offline)
                return@launch
            }

            try {
                val cachedEntities = restaurantDao.getAllRestaurants().firstOrNull()
                val localResto = cachedEntities?.map { it.toDomain() }?.find { it.id == restaurantId }

                if (localResto != null) {
                    _deepLinkRestaurant.value = localResto
                    return@launch
                }

                val response = apiService.getRestaurantById(restaurantId)

                if (response.success) {
                    _deepLinkRestaurant.value = response.data.toEntity().toDomain()
                } else {
                    _errorEvents.emit(R.string.error_deeplink_not_found)
                }

            } catch (e: Exception) {
                _errorEvents.emit(R.string.error_deeplink_generic)
                e.printStackTrace()
            }
        }
    }
}

private data class FilterUiParams(
    val query: String,
    val region: String,
    val type: String,
    val onlyOpen: Boolean,
    val onlyPmr: Boolean
)