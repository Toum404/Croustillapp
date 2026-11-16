package fr.croustillapp.visual.restaurant

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.location.Location
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.croustillapp.R
import fr.croustillapp.data.database.AppDatabase
import fr.croustillapp.data.helper.FilterUiParams
import fr.croustillapp.data.helper.RestaurantFilterHelper
import fr.croustillapp.data.model.DailyMenuDto
import fr.croustillapp.data.model.GeoJsonFeatureCollection
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.data.model.UiCategory
import fr.croustillapp.data.model.UiMenu
import fr.croustillapp.data.model.UiPlat
import fr.croustillapp.data.model.UiRepas
import fr.croustillapp.data.model.toDomain
import fr.croustillapp.data.network.NetworkMonitor
import fr.croustillapp.data.network.RetrofitClient
import fr.croustillapp.data.repository.FavoriteManager
import fr.croustillapp.data.repository.RepositoryLocation
import fr.croustillapp.data.repository.RepositoryRestaurant
import fr.croustillapp.extra.ShortcutHelper
import fr.croustillapp.ui.theme.AppDimens
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
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    // =============================================================================================
    // 1. REPOSITORIES, MANAGERS & PREFERENCES
    // =============================================================================================

    private val sharedPreferences = application.getSharedPreferences("croustillapp_prefs", Context.MODE_PRIVATE)

    private val restaurantRepository = RepositoryRestaurant(
        restaurantDao = AppDatabase.getDatabase(application).restaurantDao(),
        apiService = RetrofitClient.getService(application),
        sharedPreferences = sharedPreferences
    )

    private val locationRepository = RepositoryLocation(application)
    private val favoriteManager = FavoriteManager(application)
    private val networkMonitor = NetworkMonitor(application)

    private var locationReceiver: BroadcastReceiver? = null
    private var isCurrentlyLoading = false
    private var wasOffline = false

    // =============================================================================================
    // 2. ÉTATS UI & FILTRES (UI STATES & FILTERS)
    // =============================================================================================

    private val _searchText = MutableStateFlow("")
    private val _selectedRegion = MutableStateFlow("Toutes")
    private val _selectedType = MutableStateFlow("Tous")
    private val _showOnlyOpen = MutableStateFlow(false)
    private val _showOnlyPmr = MutableStateFlow(false)
    private val _showOnlyIzly = MutableStateFlow(false)

    val searchText = _searchText.asStateFlow()
    val selectedRegion = _selectedRegion.asStateFlow()
    val selectedType = _selectedType.asStateFlow()
    val showOnlyOpen = _showOnlyOpen.asStateFlow()
    val showOnlyPmr = _showOnlyPmr.asStateFlow()
    val showOnlyIzly = _showOnlyIzly.asStateFlow()

    private val _selectedCrousId = MutableStateFlow<Int?>(null)
    val selectedCrousId: StateFlow<Int?> = _selectedCrousId.asStateFlow()

    private val _selectedZone = MutableStateFlow<String?>(null)
    val selectedZone: StateFlow<String?> = _selectedZone.asStateFlow()

    private val _newRestaurantIds = MutableStateFlow<Set<String>>(emptySet())
    val newRestaurantIds: StateFlow<Set<String>> = _newRestaurantIds.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    // =============================================================================================
    // 3. LOGIQUE DE LOCALISATION (LOCATION MANAGEMENT)
    // =============================================================================================

    private val _isCustomLocationEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("is_custom_location_enabled", false)
    )
    val isCustomLocationEnabled: StateFlow<Boolean> = _isCustomLocationEnabled.asStateFlow()

    private val _userLocation = MutableStateFlow(
        if (sharedPreferences.getBoolean("is_custom_location_enabled", false)) {
            Pair(
                sharedPreferences.getFloat("custom_lat", 0f).toDouble(),
                sharedPreferences.getFloat("custom_lon", 0f).toDouble()
            )
        } else {
            null
        }
    )
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _isPrecisionExact = MutableStateFlow(false)
    val isPrecisionExact = _isPrecisionExact.asStateFlow()

    private val _isLocationEnabledOnDevice = MutableStateFlow(true)
    val isLocationEnabledOnDevice = _isLocationEnabledOnDevice.asStateFlow()

    private val _hasLocationPermissions = MutableStateFlow(locationRepository.hasLocationPermissions())

    fun checkLocationPermissionAndFetch() {
        if (_isCustomLocationEnabled.value) {
            val lat = sharedPreferences.getFloat("custom_lat", 46.49388f).toDouble()
            val lon = sharedPreferences.getFloat("custom_lon", 2.602778f).toDouble()
            _userLocation.value = Pair(lat, lon)
            _isPrecisionExact.value = true
            return
        }

        val hasPermission = locationRepository.hasLocationPermissions()

        if (hasPermission) {
            _isPrecisionExact.value = locationRepository.isFineLocationGranted()
            val isEnabled = locationRepository.isLocationEnabled()
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

    fun refreshLocationPermissions() {
        _hasLocationPermissions.value = locationRepository.hasLocationPermissions()
        checkLocationPermissionAndFetch()
    }

    fun saveAndEnableCustomLocation(lat: Double, lon: Double) {
        sharedPreferences.edit {
            putBoolean("is_custom_location_enabled", true)
            putFloat("custom_lat", lat.toFloat())
            putFloat("custom_lon", lon.toFloat())
        }
        _isCustomLocationEnabled.value = true
        _userLocation.value = Pair(lat, lon)
        _isPrecisionExact.value = true
    }

    fun disableCustomLocation() {
        sharedPreferences.edit { putBoolean("is_custom_location_enabled", false) }
        _isCustomLocationEnabled.value = false
        checkLocationPermissionAndFetch()
    }

    // =============================================================================================
    // 4. GESTION DE LA CARTE & GEOJSON (MAP & GEOJSON)
    // =============================================================================================

    private val _geoJsonData = MutableStateFlow<GeoJsonFeatureCollection?>(null)
    val geoJsonData: StateFlow<GeoJsonFeatureCollection?> = _geoJsonData.asStateFlow()

    private val _isMapLoading = MutableStateFlow(true)
    val isMapLoading: StateFlow<Boolean> = _isMapLoading.asStateFlow()

    // =============================================================================================
    // 5. RESTAURANTS & FLUX DE DONNÉES (RESTAURANTS & FLOWS)
    // =============================================================================================

    private val allRestaurants: StateFlow<List<Restaurant>> = restaurantRepository.allRestaurants
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteIds: StateFlow<Set<String>> = favoriteManager.favoriteIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val typesList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Tous") + list.map { it.type }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tous"))

    val regionsList: StateFlow<List<String>> = allRestaurants
        .map { list -> listOf("Toutes") + list.map { it.region }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Toutes"))

    private val filterBooleansFlow = combine(_showOnlyOpen, _showOnlyPmr, _showOnlyIzly) { open, pmr, izly ->
        Triple(open, pmr, izly)
    }

    @OptIn(FlowPreview::class)
    private val filteredParamsFlow = combine(
        _searchText.debounce(333.milliseconds).distinctUntilChanged(),
        _selectedRegion,
        _selectedType,
        filterBooleansFlow
    ) { query, region, type, booleans ->
        FilterUiParams(
            query = query,
            region = region,
            type = type,
            onlyOpen = booleans.first,
            onlyPmr = booleans.second,
            onlyIzly = booleans.third
        )
    }

    val restaurantsForSelectedCrous: StateFlow<List<Restaurant>> = combine(
        allRestaurants,
        _selectedCrousId
    ) { restaurants, crousId ->
        if (crousId == null) {
            emptyList()
        } else {
            restaurants.filter { it.regionCode == crousId }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableZones: StateFlow<List<String>> = combine(
        restaurantsForSelectedCrous,
        _selectedCrousId
    ) { restaurants, crousId ->
        if (crousId == null) {
            emptyList()
        } else {
            restaurants.map { it.zone }.distinct().sorted()
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val restaurantsForSelectedCrousAndZone: StateFlow<List<Restaurant>> = combine(
        restaurantsForSelectedCrous,
        _selectedZone
    ) { restaurants, zone ->
        val filtered = if (zone == null) {
            restaurants
        } else {
            restaurants.filter { it.zone.equals(zone, ignoreCase = true) }
        }

        filtered.sortedWith(
            compareBy<Restaurant> { it.zone }
                .thenBy { it.name }
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        restaurantsWithDistance,
        filteredParamsFlow,
        favoriteIds,
        newRestaurantIds
    ) { list, params, favorites, newIds ->
        RestaurantFilterHelper.filterAndSort(list, params, favorites, newIds)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =============================================================================================
    // 6. INITIALISATION & CYCLE DE VIE (INIT & LIFECYCLE)
    // =============================================================================================

    init {
        viewModelScope.launch {
            favoriteManager.favoriteIds.collect { favoriteIds ->
                if (favoriteIds.isNotEmpty()) {
                    val favoriteEntities = restaurantRepository.getRestaurantsByIds(favoriteIds.toList())
                    ShortcutHelper.updateFavoriteShortcuts(getApplication(), favoriteEntities)
                } else {
                    ShortcutManagerCompat.removeAllDynamicShortcuts(getApplication())
                }
            }
        }

        locationReceiver = locationRepository.registerProviderReceiver {
            if (locationRepository.isLocationEnabled()) {
                checkLocationPermissionAndFetch()
            }
        }

        viewModelScope.launch {
            restaurantRepository.getAllRestaurantEntities().collect { _ ->
                if (_isInitialLoading.value) {
                    _isInitialLoading.value = false
                }
            }
        }

        checkAndLoadData()

        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .collect { online ->
                    if (online) {
                        if (wasOffline) {
                            wasOffline = false
                            checkAndLoadData()
                        }
                    } else {
                        wasOffline = true
                    }
                }
        }

        checkLocationPermissionAndFetch()

        viewModelScope.launch {
            _isMapLoading.value = true
            _geoJsonData.value = restaurantRepository.loadRegionsGeoJson(getApplication())
            _isMapLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationReceiver?.let { locationRepository.unregisterReceiver(it) }
    }

    // =============================================================================================
    // 7. CHARGEMENT ET SYNCHRONISATION DES DONNÉES (DATA LOADING)
    // =============================================================================================

    private fun checkAndLoadData() {
        if (isCurrentlyLoading) return

        viewModelScope.launch {
            val count = restaurantRepository.getRestaurantsCount()

            if (count == 0) {
                val isOnline = networkMonitor.isOnline.first()
                if (isOnline) {
                    loadData(forceRefresh = true)
                } else {
                    isCurrentlyLoading = true
                    _isLoading.value = true
                    try {
                        restaurantRepository.loadRestaurantsFromAssetJson(getApplication())
                    } finally {
                        _isLoading.value = false
                        isCurrentlyLoading = false
                    }
                }
            } else {
                loadData(forceRefresh = false)
            }
        }
    }

    private suspend fun loadData(forceRefresh: Boolean) {
        if (isCurrentlyLoading) return
        isCurrentlyLoading = true

        try {
            if (forceRefresh) {
                _isLoading.value = true

                val existingEntities = restaurantRepository.getAllRestaurantEntities().first()
                val existingIds = existingEntities.map { it.id }.toSet()
                val wasDatabaseEmpty = existingIds.isEmpty()

                val success = restaurantRepository.refreshRestaurantsIfPossible()

                if (success && !wasDatabaseEmpty) {
                    val updatedEntities = restaurantRepository.getAllRestaurantEntities().first()
                    val updatedIds = updatedEntities.map { id -> id.id }.toSet()
                    val newlyAdded = updatedIds - existingIds
                    if (newlyAdded.isNotEmpty()) {
                        _newRestaurantIds.value += newlyAdded
                    }
                }
            }
            restaurantRepository.refreshStatusesIfNeeded()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        } finally {
            _isLoading.value = false
            isCurrentlyLoading = false
        }
    }

    // =============================================================================================
    // 8. MENUS & DEEP LINKS
    // =============================================================================================

    private val _menuState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val menuState: StateFlow<MenuUiState> = _menuState.asStateFlow()

    private var lastLoadedRestaurantId: String? = null
    private var menuJob: Job? = null

    sealed class MenuUiState {
        object Idle : MenuUiState()
        object Loading : MenuUiState()
        data class Success(val data: List<UiMenu>) : MenuUiState()
        object Error : MenuUiState()
    }

    fun loadMenu(restaurantId: String) {
        if (lastLoadedRestaurantId == restaurantId && _menuState.value is MenuUiState.Success) return

        menuJob?.cancel()
        menuJob = viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            lastLoadedRestaurantId = restaurantId

            try {
                delay(1000.milliseconds)
                val response = restaurantRepository.getMenu(restaurantId)
                if (response.success) {
                    val uiMenus = mapToUiMenus(response.data)
                    _menuState.value = MenuUiState.Success(uiMenus)
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

    private fun mapToUiMenus(dtos: List<DailyMenuDto>): List<UiMenu> {
        return dtos.map { dto ->
            UiMenu(
                date = dto.date,
                repas = dto.repas.map { repasDto ->
                    UiRepas(
                        type = repasDto.type,
                        categories = repasDto.categories.map { catDto ->
                            UiCategory(
                                libelle = catDto.libelle,
                                plats = catDto.plats.map { platDto ->
                                    val parsed = parsePlatWithPoints(platDto.libelle)
                                    UiPlat(
                                        libelle = parsed.libelle,
                                        points = parsed.points
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    private data class ParsedPlat(val libelle: String, val points: String?)

    private fun parsePlatWithPoints(rawLibelle: String): ParsedPlat {
        val match = Regex("""(.*?)\b(\d+)\s*points?\b""", RegexOption.IGNORE_CASE).find(rawLibelle)
        return if (match != null) {
            ParsedPlat(
                libelle = match.groupValues[1].trim().trimEnd('-', ':').trim(),
                points = match.groupValues[2]
            )
        } else {
            ParsedPlat(libelle = rawLibelle.trim(), points = null)
        }
    }

    private val _deepLinkRestaurant = MutableStateFlow<Restaurant?>(null)
    val deepLinkRestaurant = _deepLinkRestaurant.asStateFlow()

    private val _deepLinkErrorEvent = MutableStateFlow<Int?>(null)
    val deepLinkErrorEvent = _deepLinkErrorEvent.asStateFlow()

    fun clearDeepLinkError() { _deepLinkErrorEvent.value = null }

    fun loadSingleRestaurantFromDeepLink(restaurantId: String?) {
        viewModelScope.launch {
            if (restaurantId.isNullOrEmpty()) {
                _deepLinkErrorEvent.value = R.string.error_restaurant_txt
                return@launch
            }

            try {
                val cachedEntities = restaurantRepository.getAllRestaurantEntities().firstOrNull()
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

                _deepLinkErrorEvent.value = R.string.error_restaurant_txt

            } catch (_: Exception) {
                // ignore
            }
        }
    }

    // =============================================================================================
    // 9. ACTIONS UTILISATEUR & PRÉFÉRENCES (ACTIONS & PREFERENCES)
    // =============================================================================================

    fun updateSearchText(query: String) { _searchText.value = query }
    fun updateRegion(region: String) { _selectedRegion.value = region }
    fun updateType(type: String) { _selectedType.value = type }
    fun toggleOnlyOpen(show: Boolean) { _showOnlyOpen.value = show }
    fun toggleOnlyPmr(show: Boolean) { _showOnlyPmr.value = show }
    fun toggleOnlyIzly(show: Boolean) { _showOnlyIzly.value = show }

    fun onZoneSelected(zone: String?) {
        _selectedZone.value = if (_selectedZone.value == zone) null else zone
    }

    fun toggleCrousSelection(crousId: Int) {
        _selectedCrousId.value = if (_selectedCrousId.value == crousId) null else crousId
        _selectedZone.value = null
    }

    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch {
            val currentFavorites = favoriteIds.value
            val isAlreadyFavorite = currentFavorites.contains(restaurantId)

            if (!isAlreadyFavorite) {
                if (currentFavorites.size >= AppDimens.FAVORIS_MAX) {
                    val formattedMessage = getApplication<Application>().getString(
                        R.string.error_favoris_max,
                        AppDimens.FAVORIS_MAX
                    )
                    _errorEvents.emit(formattedMessage)
                } else {
                    favoriteManager.saveFavorites(currentFavorites + restaurantId)
                }
            } else {
                favoriteManager.saveFavorites(currentFavorites - restaurantId)
            }
        }
    }

    private val _appLanguage = MutableStateFlow(
        sharedPreferences.getString("app_language", "Auto") ?: "Auto"
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        sharedPreferences.edit { putString("app_language", language) }

        val tag = when (language) {
            "Français" -> "fr"
            "English" -> "en"
            "Español" -> "es"
            "Deutsch" -> "de"
            else -> ""
        }

        val appLocale = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }

        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private val _linkLanguage = MutableStateFlow(
        sharedPreferences.getString("link_language", "Auto") ?: "Auto"
    )
    val linkLanguage: StateFlow<String> = _linkLanguage.asStateFlow()

    fun setLinkLanguage(language: String) {
        _linkLanguage.value = language
        sharedPreferences.edit { putString("link_language", language) }
    }

    private val _themeOption = MutableStateFlow(
        sharedPreferences.getString("theme_option", "system") ?: "system"
    )
    val themeOption: StateFlow<String> = _themeOption.asStateFlow()

    fun setThemeOption(option: String) {
        _themeOption.value = option
        sharedPreferences.edit { putString("theme_option", option) }
    }

    private val _isDynamicColorEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("is_dynamic_color_enabled", false)
    )
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    fun toggleDynamicColor(enabled: Boolean) {
        _isDynamicColorEnabled.value = enabled
        sharedPreferences.edit { putBoolean("is_dynamic_color_enabled", enabled) }
    }

    private val _isPureBlackEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("is_pure_black_enabled", false)
    )
    val isPureBlackEnabled: StateFlow<Boolean> = _isPureBlackEnabled.asStateFlow()

    fun togglePureBlack(enabled: Boolean) {
        _isPureBlackEnabled.value = enabled
        sharedPreferences.edit { putBoolean("is_pure_black_enabled", enabled) }
    }

    private val _isZoomButtons = MutableStateFlow(
        sharedPreferences.getBoolean("is_zoom_buttons_enabled", true)
    )
    val isZoomButtons: StateFlow<Boolean> = _isZoomButtons.asStateFlow()

    fun toggleExtraInfo(enabled: Boolean) {
        _isZoomButtons.value = enabled
        sharedPreferences.edit { putBoolean("is_zoom_buttons_enabled", enabled) }
    }

    private val _isOldNamesEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("is_old_names_enabled", false)
    )
    val isOldNamesEnabled: StateFlow<Boolean> = _isOldNamesEnabled.asStateFlow()

    fun toggleOldNames(enabled: Boolean) {
        _isOldNamesEnabled.value = enabled
        sharedPreferences.edit { putBoolean("is_old_names_enabled", enabled) }
    }

    private val _isMinimalModeEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("is_minimal_mode_enabled", false)
    )
    val isMinimalModeEnabled: StateFlow<Boolean> = _isMinimalModeEnabled.asStateFlow()

    fun toggleMinimalMode(enabled: Boolean) {
        _isMinimalModeEnabled.value = enabled
        sharedPreferences.edit { putBoolean("is_minimal_mode_enabled", enabled) }
    }

    fun getLocalizedUrlSegment(userLinkPreference: String): String {
        return if (userLinkPreference == "Auto") {
            val systemLocale = AppCompatDelegate.getApplicationLocales().get(0)
                ?: java.util.Locale.getDefault()
            if (systemLocale.language.startsWith("fr")) "fr" else "en"
        } else {
            if (userLinkPreference == "Français") "fr" else "en"
        }
    }
}