package fr.croustillapp.features.elements

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.croustillapp.R
import fr.croustillapp.core.database.AppDatabase
import fr.croustillapp.core.network.NetworkMonitor
import fr.croustillapp.core.network.RetrofitClient
import fr.croustillapp.features.data.DailyMenuDto
import fr.croustillapp.features.data.FavoriteManager
import fr.croustillapp.features.data.FilterUiParams
import fr.croustillapp.features.data.LocationRepository
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.features.data.RestaurantFilterHelper
import fr.croustillapp.features.data.RestaurantRepository
import fr.croustillapp.features.data.toDomain
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

sealed interface ErrorType {
    object None : ErrorType
    object NoInternet : ErrorType
    object ServerError : ErrorType
}

/**
 * FR: Master ViewModel orchestrant le cycle de vie des donnees, le filtrage, le geopositionnement et le cache.
 * EN: Master ViewModel driving core data flows, cross-filtering matrices, location checks, and cache pipelines.
 */
class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    // Gestionnaires de localisation et recepteurs d'evenements GPS / Location manager and GPS broadcast receivers
    private val locationRepository = LocationRepository(application)
    private var locationReceiver: BroadcastReceiver? = null

    // FR: Configuration des sources de donnees locales (Room / SharedPreferences) et distantes (API)
    // EN: Setup for local data sources (Room / SharedPreferences) and remote API
    private val sharedPreferences = application.getSharedPreferences("croustillapp_prefs", Context.MODE_PRIVATE)
    private val restaurantRepository = RestaurantRepository(
        restaurantDao = AppDatabase.getDatabase(application).restaurantDao(),
        apiService = RetrofitClient.getService(application),
        sharedPreferences = sharedPreferences
    )

    private val database = AppDatabase.getDatabase(application)
    private val restaurantDao = database.restaurantDao()
    private val apiService = RetrofitClient.getService(application)

    private val favoriteManager = FavoriteManager(application)
    private val networkMonitor = NetworkMonitor(application)

    // Flux d'evenements et d'etats d'erreur / Error events and state flows
    private val _errorEvents = MutableSharedFlow<Int>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _errorType = MutableStateFlow<ErrorType>(ErrorType.None)
    val errorType = _errorType.asStateFlow()

    // Etats de l'interface pour les filtres (recherche, region, type, options)
    // UI states for filters (search query, region, type, options)
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

    // Etats lies a la geolocalisation de l'utilisateur / User geolocation-related states
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isPrecisionExact = MutableStateFlow(false)
    val isPrecisionExact = _isPrecisionExact.asStateFlow()

    private val _isLocationEnabledOnDevice = MutableStateFlow(true)
    val isLocationEnabledOnDevice = _isLocationEnabledOnDevice.asStateFlow()

    // Liste des identifiants favoris mise en cache / Cached set of favorite identifiers
    val favoriteIds: StateFlow<Set<String>> = favoriteManager.favoriteIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var isCurrentlyLoading = false

    // Gestion de l'etat UI du menu journalier / Daily menu UI state management
    private val _menuState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val menuState: StateFlow<MenuUiState> = _menuState.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private var lastLoadedRestaurantId: String? = null
    private var menuJob: Job? = null

    /**
     * FR: Etats possibles pour le chargement du menu d'un restaurant.
     * EN: Possible states for loading a restaurant's menu.
     */
    sealed class MenuUiState {
        object Idle : MenuUiState()
        object Loading : MenuUiState()
        data class Success(val data: List<DailyMenuDto>) : MenuUiState()
        object Error : MenuUiState()
    }

    /**
     * FR: Charge le menu d'un restaurant specifique de maniere asynchrone.
     * EN: Loads a specific restaurant's menu asynchronously.
     */
    fun loadMenu(restaurantId: String) {
        if (lastLoadedRestaurantId == restaurantId && _menuState.value is MenuUiState.Success) return

        menuJob?.cancel()
        menuJob = viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            lastLoadedRestaurantId = restaurantId

            try {
                delay(1000.milliseconds)
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

    // Recuperation de tous les restaurants depuis la base de donnees locale / Fetches all restaurants from local DB
    private val allRestaurants: StateFlow<List<Restaurant>> = restaurantRepository.allRestaurants
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Listes dynamiques des types et regions disponibles / Dynamic lists of available types and regions
    val typesList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Tous") + list.map { it.type }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tous"))

    val regionsList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Toutes") + list.map { it.region }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Toutes"))

    // Flux combine des parametres de filtres avec debounce sur la recherche / Combined filter params flow with search debounce
    @OptIn(FlowPreview::class)
    private val filteredParamsFlow = combine(
        _searchText.debounce(333.milliseconds).distinctUntilChanged(),
        _selectedRegion,
        _selectedType,
        _showOnlyOpen,
        _showOnlyPmr
    ) { query, region, type, onlyOpen, onlyPmr ->
        FilterUiParams(query, region, type, onlyOpen, onlyPmr)
    }

    // Calcule la distance entre l'utilisateur et chaque restaurant / Calculates distance between user and each restaurant
    private val restaurantsWithDistance: StateFlow<List<Restaurant>> = combine(
        allRestaurants,
        _userLocation
    ) { list, userLoc ->
        if (userLoc != null) {
            val localHolder = FloatArray(1)
            list.map { resto ->
                Location.distanceBetween(userLoc.first, userLoc.second, resto.latitude, resto.longitude, localHolder)
                resto.copy(distance = localHolder[0])
            }
        } else {
            list.map { it.copy(distance = null) }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Liste finale des restaurants filtres et tries / Final filtered and sorted list of restaurants
    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        restaurantsWithDistance,
        filteredParamsFlow
    ) { list, params ->
        RestaurantFilterHelper.filterAndSort(list, params)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var wasOffline = false

    init {
        // Ecoute les changements d'etat du GPS / Listens to GPS provider status changes
        locationReceiver = locationRepository.registerProviderReceiver {
            if (locationRepository.isGpsProviderEnabled()) {
                checkLocationPermissionAndFetch()
            }
        }

        // Met a jour l'indicateur de chargement initial selon la base de donnees / Updates initial load flag based on DB
        viewModelScope.launch {
            restaurantDao.getAllRestaurants().collect { _ ->
                if (_isInitialLoading.value) {
                    _isInitialLoading.value = false
                }
            }
        }

        // Surveille la connectivite reseau en temps reel / Monitors network connectivity in real-time
        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .collect { online ->
                    if (online) {
                        if (wasOffline) {
                            wasOffline = false
                        }
                        checkAndLoadData()
                    } else {
                        wasOffline = true

                        val count = withContext(Dispatchers.IO) { restaurantDao.getRestaurantsCount() }
                        if (count == 0) {
                            _errorType.value = ErrorType.NoInternet
                        }
                    }
                }
        }
        checkLocationPermissionAndFetch()
    }

    // Nettoyage des recepteurs pour eviter les fuites de memoire / Unregisters receiver to prevent memory leaks
    override fun onCleared() {
        super.onCleared()
        locationReceiver?.let { locationRepository.unregisterReceiver(it) }
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

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateSearchText(query: String) { _searchText.value = query }
    fun updateRegion(region: String) { _selectedRegion.value = region }
    fun updateType(type: String) { _selectedType.value = type }
    fun toggleOnlyOpen(show: Boolean) { _showOnlyOpen.value = show }
    fun toggleOnlyPmr(show: Boolean) { _showOnlyPmr.value = show }

    // Chargement et rafraîchissement des donnees distantes / Fetches and refreshes remote data
    private suspend fun loadData(forceRefresh: Boolean) {
        if (isCurrentlyLoading) return
        isCurrentlyLoading = true

        try {
            if (forceRefresh) {
                _isLoading.value = true
                _errorType.value = ErrorType.None
                restaurantRepository.refreshRestaurantsIfPossible()
            }
            restaurantRepository.refreshStatusesIfNeeded()
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val count = restaurantRepository.getRestaurantsCount()
            if (count == 0) {
                _errorType.value = if (e is HttpException) {
                    ErrorType.ServerError
                } else {
                    ErrorType.NoInternet
                }
            }
        } finally {
            _isLoading.value = false
            isCurrentlyLoading = false
        }
    }

    // Gestion des favoris avec limite maximale / Managesfavorites with a max limit
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

    private val _deepLinkErrorEvent = MutableStateFlow<Int?>(null)
    val deepLinkErrorEvent = _deepLinkErrorEvent.asStateFlow()

    fun clearDeepLinkError() { _deepLinkErrorEvent.value = null }

    // Charge un restaurant specifique via un lien profond (Deep Link) / Loads a specific restaurant via a deep link
    fun loadSingleRestaurantFromDeepLink(restaurantId: String?) {
        viewModelScope.launch {
            if (restaurantId.isNullOrEmpty()) {
                _deepLinkErrorEvent.value = R.string.deeplink_error_not_found
                return@launch
            }

            try {
                val cachedEntities = restaurantDao.getAllRestaurants().firstOrNull()
                val localResto = cachedEntities?.map { it.toDomain() }?.find { it.id == restaurantId }

                if (localResto != null) {
                    _deepLinkRestaurant.value = localResto
                    return@launch
                }

                val isOnlineNow = networkMonitor.isOnline.first()
                if (isOnlineNow) {
                    val remoteResto = restaurantRepository.fetchRestaurantById(restaurantId)
                    if (remoteResto != null) {
                        _deepLinkRestaurant.value = remoteResto
                        return@launch
                    }
                }

                _deepLinkErrorEvent.value = R.string.deeplink_error_not_found

            } catch (e: Exception) {
                _deepLinkErrorEvent.value = R.string.deeplink_error_not_found
                e.printStackTrace()
            }
        }
    }

    // Verifie les permissions de localisation et recupere les coordonnees GPS / Checks location permissions and gets GPS coordinates
    fun checkLocationPermissionAndFetch() {
        val hasPermission = locationRepository.hasLocationPermissions()

        if (hasPermission) {
            _isPrecisionExact.value = locationRepository.isFineLocationGranted()
            val isEnabled = locationRepository.isGpsProviderEnabled()
            _isLocationEnabledOnDevice.value = isEnabled

            if (!isEnabled) {
                _userLocation.value = null
                _isPrecisionExact.value = false
                return
            }

            viewModelScope.launch {
                val coords = locationRepository.fetchCurrentLocation()
                _userLocation.value = coords
            }
        } else {
            _isLocationEnabledOnDevice.value = true
            _userLocation.value = null
            _isPrecisionExact.value = false
        }
    }
}