package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Holding
import com.example.data.InvestmentRepository
import com.example.data.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = InvestmentRepository(db.investmentDao())

    // Currently selected tab: "Home", "Markets", "Portfolio", "Settings"
    private val _currentTab = MutableStateFlow("Home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Screen state variables
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Real-time market prices for simulation
    private val _marketPrices = MutableStateFlow(
        mapOf(
            "AAPL" to 172.50,
            "GOOG" to 142.20,
            "TSLA" to 180.40,
            "VTSAX" to 112.50,
            "VTIAX" to 51.20,
            "RET-401K" to 158400.00,
            "RET-IRA" to 81.40
        )
    )
    val marketPrices: StateFlow<Map<String, Double>> = _marketPrices.asStateFlow()

    // Stock price trend direction map (for flashes or color cues)
    private val _priceChanges = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val priceChanges: StateFlow<Map<String, Boolean>> = _priceChanges.asStateFlow()

    // Selected chart timeframe filter ("1D", "1W", "1M", "3M", "1Y", "ALL")
    private val _chartFilter = MutableStateFlow("1M")
    val chartFilter: StateFlow<String> = _chartFilter.asStateFlow()

    // Live list of holdings from Room Database
    val holdings: StateFlow<List<Holding>> = repository.allHoldings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions log from Room Database
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulation enabled state toggle
    private val _simulationEnabled = MutableStateFlow(true)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            startPriceSimulation()
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectChartFilter(filter: String) {
        _chartFilter.value = filter
    }

    fun setSimulationEnabled(enabled: Boolean) {
        _simulationEnabled.value = enabled
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // High fidelity market simulation ticker
    private fun startPriceSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(3500) // update every 3.5 seconds
                if (_simulationEnabled.value) {
                    val current = _marketPrices.value
                    val updatedChanges = mutableMapOf<String, Boolean>()
                    val updated = current.mapValues { (symbol, price) ->
                        // Standard assets fluctuate slightly (+- 0.1% to 1.5%)
                        val percent = if (symbol.startsWith("RET-")) {
                            Random.nextDouble(-0.0005, 0.0008) // retirement safer/slower
                        } else {
                            Random.nextDouble(-0.008, 0.010)
                        }
                        val newPrice = (price * (1 + percent))
                        val formattedPrice = Math.round(newPrice * 100.0) / 100.0
                        updatedChanges[symbol] = percent >= 0
                        formattedPrice
                    }
                    _priceChanges.value = updatedChanges
                    _marketPrices.value = updated
                }
            }
        }
    }

    // Trade simulator: buys or sells assets
    fun executeTrade(
        symbol: String,
        name: String,
        category: String,
        multiplierUnit: Double, // amount of shares
        price: Double,
        isBuy: Boolean
    ) {
        viewModelScope.launch {
            if (multiplierUnit <= 0 || price <= 0) {
                _statusMessage.value = "Error: Invalid quantity or price."
                return@launch
            }

            val list = holdings.value
            val existing = list.find { it.symbol == symbol }

            if (isBuy) {
                if (existing != null) {
                    // Average cost recalculation
                    val totalCost = (existing.quantity * existing.averagePrice) + (multiplierUnit * price)
                    val newQty = existing.quantity + multiplierUnit
                    val newAvg = totalCost / newQty
                    repository.updateHolding(
                        existing.copy(
                            quantity = newQty,
                            averagePrice = Math.round(newAvg * 1000.0) / 1000.0
                        )
                    )
                } else {
                    repository.insertHolding(
                        Holding(
                            symbol = symbol,
                            name = name,
                            quantity = multiplierUnit,
                            averagePrice = price,
                            category = category
                        )
                    )
                }
                repository.insertTransaction(
                    Transaction(
                        symbol = symbol,
                        name = name,
                        quantity = multiplierUnit,
                        price = price,
                        type = "BUY",
                        category = category
                    )
                )
                _statusMessage.value = "Successfully purchased $multiplierUnit units of $symbol at $${String.format("%.2f", price)}"
            } else {
                // Sell logic
                if (existing == null || existing.quantity < multiplierUnit) {
                    _statusMessage.value = "Error: Insufficient holdings to sell."
                    return@launch
                }
                val newQty = existing.quantity - multiplierUnit
                if (newQty <= 0.0) {
                    repository.deleteHolding(existing)
                } else {
                    repository.updateHolding(existing.copy(quantity = newQty))
                }
                repository.insertTransaction(
                    Transaction(
                        symbol = symbol,
                        name = name,
                        quantity = multiplierUnit,
                        price = price,
                        type = "SELL",
                        category = category
                    )
                )
                _statusMessage.value = "Successfully sold $multiplierUnit units of $symbol at $${String.format("%.2f", price)}"
            }
        }
    }

    // Seed/Wipe controls for sandbox testing
    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            repository.seedInitialDataIfNeeded()
            _statusMessage.value = "Database successfully reset to original portfolio defaults!"
        }
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            _statusMessage.value = "All portfolio data successfully wiped clean!"
        }
    }
}
