package com.fullpack.panictrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TradingBotApp() }
    }
}

@Composable
private fun TradingBotApp(vm: TradingBotViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    var symbolInput by remember { mutableStateOf(uiState.config.symbol) }
    var riskInput by remember { mutableStateOf(uiState.config.riskPercent.toString()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Trading Bot (Paper Mode)", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = symbolInput,
                onValueChange = { symbolInput = it },
                label = { Text("Symbol") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = riskInput,
                onValueChange = { riskInput = it },
                label = { Text("Risk % per trade (max 5)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.updateSymbol(symbolInput)
                    vm.updateRiskPercent(riskInput)
                    vm.start()
                }) { Text("Start") }
                Button(onClick = vm::stop) { Text("Stop") }
                Button(onClick = vm::reset) { Text("Reset") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pair: ${uiState.config.symbol}")
                    Text("Last Price: ${"%.2f".format(uiState.lastPrice)}")
                    Text(
                        "Balance: $${"%.2f".format(uiState.balance)}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Trades: ${uiState.totalTrades} | Win rate: ${"%.1f".format(uiState.winRate)}%")
                    Text("Drawdown: ${"%.2f".format(uiState.drawdown)}%")
                    Text("Status: ${if (uiState.isRunning) "Running" else "Stopped"}")
                }
            }

            Text("Trade Log")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.logs.take(50)) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(log, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
