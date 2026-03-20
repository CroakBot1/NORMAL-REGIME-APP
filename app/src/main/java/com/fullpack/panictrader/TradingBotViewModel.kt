package com.fullpack.panictrader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

data class BotConfig(
    val symbol: String = "BTCUSDT",
    val riskPercent: Double = 1.0,
    val fastWindow: Int = 5,
    val slowWindow: Int = 14
)

data class TradeEvent(
    val side: String,
    val entry: Double,
    val exit: Double,
    val pnl: Double
)

data class TradingUiState(
    val balance: Double = 1000.0,
    val equityHigh: Double = 1000.0,
    val isRunning: Boolean = false,
    val config: BotConfig = BotConfig(),
    val lastPrice: Double = 100.0,
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val logs: List<String> = emptyList()
) {
    val winRate: Double get() = if (totalTrades == 0) 0.0 else (wins * 100.0 / totalTrades)
    val drawdown: Double get() = if (equityHigh == 0.0) 0.0 else ((equityHigh - balance) / equityHigh) * 100.0
}

class TradingBotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    private var tradingJob: Job? = null

    fun start() {
        if (_uiState.value.isRunning) return

        _uiState.update {
            it.copy(isRunning = true, logs = listOf("Bot started on ${it.config.symbol}") + it.logs)
        }

        tradingJob = viewModelScope.launch {
            val prices = ArrayDeque<Double>()
            var price = _uiState.value.lastPrice

            while (_uiState.value.isRunning) {
                delay(1200)

                price *= 1 + Random.nextDouble(-0.007, 0.007)
                prices.addLast(price)

                val slow = max(_uiState.value.config.slowWindow, _uiState.value.config.fastWindow + 1)
                while (prices.size > slow + 1) prices.removeFirst()

                if (prices.size < slow + 1) {
                    _uiState.update { it.copy(lastPrice = price) }
                    continue
                }

                val cfg = _uiState.value.config
                val fastMa = prices.takeLast(cfg.fastWindow).average()
                val slowMa = prices.takeLast(cfg.slowWindow).average()
                val side = if (fastMa >= slowMa) "BUY" else "SELL"

                val change = Random.nextDouble(-0.004, 0.005)
                val exit = price * (1 + change)
                val riskDollars = _uiState.value.balance * (cfg.riskPercent / 100.0)
                val pnl = if (side == "BUY") {
                    riskDollars * ((exit - price) / price) * 10
                } else {
                    riskDollars * ((price - exit) / price) * 10
                }

                applyTrade(
                    event = TradeEvent(
                        side = side,
                        entry = price,
                        exit = exit,
                        pnl = pnl
                    )
                )
            }
        }
    }

    private fun applyTrade(event: TradeEvent) {
        _uiState.update { state ->
            val newBalance = (state.balance + event.pnl).coerceAtLeast(0.0)
            val newHigh = max(state.equityHigh, newBalance)
            state.copy(
                balance = newBalance,
                equityHigh = newHigh,
                lastPrice = event.exit,
                totalTrades = state.totalTrades + 1,
                wins = state.wins + if (event.pnl > 0) 1 else 0,
                logs = listOf(
                    "${event.side} entry ${"%.2f".format(event.entry)} -> exit ${"%.2f".format(event.exit)} | PnL ${"%.2f".format(event.pnl)}"
                ) + state.logs
            )
        }
    }

    fun stop() {
        tradingJob?.cancel()
        tradingJob = null
        _uiState.update { it.copy(isRunning = false, logs = listOf("Bot stopped") + it.logs) }
    }

    fun reset() {
        stop()
        _uiState.value = TradingUiState(logs = listOf("Bot reset to initial capital"))
    }

    fun updateSymbol(symbol: String) {
        if (symbol.isBlank()) return
        _uiState.update {
            it.copy(config = it.config.copy(symbol = symbol.trim().uppercase()))
        }
    }

    fun updateRiskPercent(risk: String) {
        val value = risk.toDoubleOrNull() ?: return
        if (value <= 0 || value > 5) return
        _uiState.update {
            it.copy(config = it.config.copy(riskPercent = value))
        }
    }

    override fun onCleared() {
        tradingJob?.cancel()
        super.onCleared()
    }
}
