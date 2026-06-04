package fr.croustillapp.features.elements

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.croustillapp.R
import fr.croustillapp.core.database.AppDatabase
import fr.croustillapp.core.database.StatusUpdatePartial
import fr.croustillapp.core.network.NetworkMonitor
import fr.croustillapp.core.network.RetrofitClient
import fr.croustillapp.features.data.DailyMenuDto
import fr.croustillapp.features.data.FavoriteManager
import fr.croustillapp.features.data.Restaurant
import fr.croustillapp.features.data.toDomain
import fr.croustillapp.features.data.toEntity
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * FR: Structure interne immuable agrégeant les critères de recherche utilisateur.
 * EN: Immutable internal payload mapping out explicit active user search filters.
 */
private data class FilterUiParams(
    val query: String,
    val region: String,
    val type: String,
    val onlyOpen: Boolean,
    val onlyPmr: Boolean
)

sealed interface ErrorType {
    object None : ErrorType
    object NoInternet : ErrorType
    object ServerError : ErrorType
}

/**
 * FR: Master ViewModel orchestrant le cycle de vie des données, le filtrage, le géopositionnement et le cache.
 * EN: Master ViewModel driving core data flows, cross-filtering matrices, location checks, and cache pipelines.
 */
class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val sharedPreferences = application.getSharedPreferences("croustillapp_prefs", Context.MODE_PRIVATE)
    private val lastUpdateKey = "last_status_update_timestamp"

    private val database = AppDatabase.getDatabase(application)
    private val restaurantDao = database.restaurantDao()
    private val apiService = RetrofitClient.getService(application)

    private val favoriteManager = FavoriteManager(application)
    private val networkMonitor = NetworkMonitor(application)

    // FR: Événements à sens unique pour afficher les Toasts d'erreurs (ex: DeepLink cassé, limite favoris).
    // EN: One-shot event stream processing error interactions (e.g., broken deeplinks, favorites limit).
    private val _errorEvents = MutableSharedFlow<Int>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val _errorType = MutableStateFlow<ErrorType>(ErrorType.None)
    val errorType = _errorType.asStateFlow()

    // FR: États mutables isolés pour capturer les entrées de filtrage de l'UI.
    // EN: Isolated mutable states backing reactive inputs from the user interface filters.
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

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isPrecisionExact = MutableStateFlow(false)
    val isPrecisionExact = _isPrecisionExact.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoriteManager.favoriteIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var isCurrentlyLoading = false

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

    /**
     * FR: Charge le menu d'un restaurant de façon asynchrone avec annulation du job précédent en cas d'appel rapide.
     * EN: Asynchronously loads a menu while cancelling previous job routines upon subsequent rapid requests.
     */
    fun loadMenu(restaurantId: String) {
        if (lastLoadedRestaurantId == restaurantId && _menuState.value is MenuUiState.Success) return

        menuJob?.cancel()
        menuJob = viewModelScope.launch {
            _menuState.value = MenuUiState.Loading
            lastLoadedRestaurantId = restaurantId

            try {
                // FR: Léger délai d'attente pour fluidifier l'UI.
                // EN: Smoothing layout throttle delay.
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

    // FR: Flux source connectant la table SQLite locale convertie en objets de domaine.
    // EN: Baseline upstream syncing local SQLite entity listings maps onto clean domain model parameters.
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

    // FR: Combine les filtres UI en y appliquant un filtre Debounce anti-rebond sur la saisie textuelle.
    // EN: Merges layout filters applying a safe debounce calculation window over keyboard search fields.
    @OptIn(FlowPreview::class)
    private val filteredParamsFlow = combine(
        _searchText.debounce(333).distinctUntilChanged(),
        _selectedRegion,
        _selectedType,
        _showOnlyOpen,
        _showOnlyPmr
    ) { query, region, type, onlyOpen, onlyPmr ->
        FilterUiParams(query, region, type, onlyOpen, onlyPmr)
    }

    // FR: Calcule en arrière-plan la distance métrique entre l'utilisateur et chaque restaurant.
    // EN: Computes background metric distance vectors separating the active user coordinate context from restaurants.
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

    /**
     * FR: Flux final observé par l'UI : Applique les filtres et trie par distance ou par nom alphabétique.
     * EN: Ultimate UI-consumed data pipeline: Applies structural criteria matrix filtering and sorts by proximity/alphabetics.
     */
    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        restaurantsWithDistance,
        filteredParamsFlow
    ) { list, params ->
        list.filter { resto ->
            val matchesSearch = params.query.isEmpty() || resto.name.contains(params.query, ignoreCase = true) || resto.id.contains(params.query)
            val matchesRegion = params.region == "Toutes" || resto.region == params.region
            val matchesType = params.type == "Tous" || resto.type == params.type
            val matchesOpen = !params.onlyOpen || resto.isOpen
            val matchesPmr = !params.onlyPmr || resto.pmr

            matchesSearch && matchesRegion && matchesType && matchesOpen && matchesPmr
        }.sortedWith { r1, r2 ->
            if (r1.distance != null && r2.distance != null) {
                r1.distance.compareTo(r2.distance)
            } else {
                r1.name.compareTo(r2.name)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FR: Variable locale pour suivre l'état de déconnexion précédent et éviter les faux positifs UI.
    // EN: Local flag to track previous disconnection state and prevent UI false positives.
    var wasOffline = false

    init {
        // FR: Écouteur réactif de connectivité réseau pour rafraîchir ou lever une alerte.
        // EN: Reactive lifecycle network connectivity listener executing data pulls or tracking errors.
        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .collect { online ->
                    if (online) {
                        // FR: Le toast ne se déclenche que si l'appareil était précédemment marqué hors-ligne.
                        // EN: The toast triggers only if the device was previously flagged as offline.
                        if (wasOffline) {
                            _errorEvents.emit(R.string.toast_mode_en_ligne)
                            wasOffline = false
                        }
                        checkAndLoadData()
                    } else {
                        wasOffline = true

                        val count = withContext(Dispatchers.IO) { restaurantDao.getRestaurantsCount() }
                        if (count == 0) {
                            _errorType.value = ErrorType.NoInternet
                        } else {
                            _errorEvents.emit(R.string.toast_mode_cache)
                        }
                    }
                }
        }
        checkLocationPermissionAndFetch()
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

    /**
     * FR: Gère le chargement initial lourd des restaurants depuis l'API distante vers la base de données locale Room.
     * EN: Handles heavy-lifting initial remote ingestion from API components down into Room database entities.
     */
    private suspend fun loadData(forceRefresh: Boolean) {
        if (isCurrentlyLoading) return
        isCurrentlyLoading = true

        try {
            if (forceRefresh) {
                _isLoading.value = true
                _errorType.value = ErrorType.None
                val fullResponse = apiService.getRestaurants()
                if (fullResponse.success) {
                    withContext(Dispatchers.IO) {
                        restaurantDao.insertAll(fullResponse.data.map { it.toEntity() })
                    }
                }
            }
            refreshOnlyStatuses()
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val count = withContext(Dispatchers.IO) { restaurantDao.getRestaurantsCount() }
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

    /**
     * FR: Rafraîchit uniquement les variables d'ouverture partielles (évite de ré-allouer tout le catalogue).
     * EN: Checks and updates isolated operational state parameters using partial payload network structures.
     */
    private suspend fun refreshOnlyStatuses() {
        val currentTime = System.currentTimeMillis()
        val lastUpdate = sharedPreferences.getLong(lastUpdateKey, 0L)
        val fiveMinutesInMs = 5 * 60 * 1000

        if (currentTime - lastUpdate < fiveMinutesInMs) return

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
        } catch (_: Exception) {}
    }

    /**
     * FR: Inverse le statut favori d'un restaurant tout en bloquant l'ajout au-delà de 6 éléments.
     * EN: Flips specific favorite item states, enforcing strict upper-bound rejection ceilings at 6 items.
     */
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

    fun clearDeepLinkRestaurant() { _deepLinkRestaurant.value = null }

    /**
     * FR: Résout de façon sécurisée les lancements par DeepLink (vérification du cache, puis récupération API).
     * EN: Resolves deep link navigation inputs (verifies local data cache targets, fallback to remote endpoint calls).
     */
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

    /**
     * FR: Évalue l'état des permissions système Android et récupère les données GPS via fonctions suspendues.
     * EN: Resolves native system runtime location permissions and acquires GPS markers via async suspension scopes.
     */
    fun checkLocationPermissionAndFetch() {
        val hasCoarse = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasCoarse || hasFine) {
            _isPrecisionExact.value = hasFine

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                viewModelScope.launch {
                    _errorEvents.emit(R.string.aucune_position)
                }
                _userLocation.value = null
                _isPrecisionExact.value = false
                return
            }

            val provider = when {
                hasFine && isGpsEnabled -> LocationManager.GPS_PROVIDER
                isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
                else -> LocationManager.PASSIVE_PROVIDER
            }

            try {
                val lastKnownLocation = locationManager.getLastKnownLocation(provider)
                var cacheUtilise = false

                if (lastKnownLocation != null) {
                    val ageDuCache = System.currentTimeMillis() - lastKnownLocation.time
                    val cacheValidity = 3 * 60 * 1000 // 3 minutes

                    if (ageDuCache < cacheValidity) {
                        _userLocation.value = Pair(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        cacheUtilise = true
                    }
                }

                if (!cacheUtilise) {
                    viewModelScope.launch {
                        try {
                            val location = suspendCancellableCoroutine { continuation ->
                                val listener = object : LocationListener {
                                    override fun onLocationChanged(loc: Location) {
                                        if (continuation.isActive) {
                                            locationManager.removeUpdates(this)
                                            continuation.resume(loc)
                                        }
                                    }
                                }

                                continuation.invokeOnCancellation {
                                    locationManager.removeUpdates(listener)
                                }

                                locationManager.requestLocationUpdates(
                                    provider,
                                    0L,
                                    0f,
                                    listener,
                                    getApplication<Application>().mainLooper
                                )
                            }

                            _userLocation.value = Pair(location.latitude, location.longitude)

                        } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            _userLocation.value = null
            _isPrecisionExact.value = false
        }
    }
}