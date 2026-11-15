package com.example.mobilesbase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.viewmodel.MobileViewModel

val OPERATORS_LIST = listOf("МТС", "Билайн", "МегаФон", "Tele2", "Йота", "СберМобайл", "Тинькофф Мобайл", "Ростелеком")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(viewModel: MobileViewModel, onDismiss: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("+7 ") }
    var operator by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var isUnlimitedMin by remember { mutableStateOf(false) }
    var gb by remember { mutableStateOf("") }
    var isUnlimitedGb by remember { mutableStateOf(false) }
    var simLocation by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить номер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Номер (+7 xxx xxx-xx-xx)") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = operator,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Оператор") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        OPERATORS_LIST.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    operator = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = payment,
                    onValueChange = { payment = it },
                    label = { Text("Оплата в месяц (₽)") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (isUnlimitedMin) "Безлимит" else minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Минуты") },
                        enabled = !isUnlimitedMin,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(checked = isUnlimitedMin, onCheckedChange = { 
                        isUnlimitedMin = it
                        if (it) minutes = "Безлимит" else minutes = ""
                    })
                    Text("Безл.")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (isUnlimitedGb) "Безлимит" else gb,
                        onValueChange = { gb = it },
                        label = { Text("ГБ") },
                        enabled = !isUnlimitedGb,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(checked = isUnlimitedGb, onCheckedChange = { 
                        isUnlimitedGb = it
                        if (it) gb = "Безлимит" else gb = ""
                    })
                    Text("Безл.")
                }
                OutlinedTextField(
                    value = simLocation,
                    onValueChange = { simLocation = it },
                    label = { Text("Где стоит SIM (напр. Телефон Слот 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (phoneNumber.isNotBlank() && operator.isNotBlank()) {
                    val newCard = MobileCard(
                        phoneNumber = phoneNumber.trim(),
                        operator = operator,
                        monthlyPayment = payment.toDoubleOrNull() ?: 0.0,
                        monthlyMinutes = if (isUnlimitedMin) "Безлимит" else (minutes.ifBlank { "0" }),
                        monthlyGb = if (isUnlimitedGb) "Безлимит" else (gb.ifBlank { "0" }),
                        simLocation = simLocation.ifBlank { "Не указано" },
                        balance = 0.0
                    )
                    viewModel.insertCard(newCard)
                    onDismiss()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun MobileCardItem(card: MobileCard, viewModel: MobileViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = card.phoneNumber, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.fetchBalanceAutomatically(card.phoneNumber) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить баланс")
                }
                IconButton(onClick = { viewModel.deleteCard(card) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(text = card.operator, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Где: ${card.simLocation}")
            Text(text = "Оплата: ${card.monthlyPayment} ₽/мес")
            Text(text = "Тариф: ${card.monthlyMinutes} мин, ${card.monthlyGb} ГБ")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Баланс: ${card.balance} ₽", 
                style = MaterialTheme.typography.titleMedium,
                color = if (card.balance < 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MobileViewModel = viewModel()) {
    val cards by viewModel.allCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Мои мобильные номера") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Нет добавленных номеров. Нажмите +, чтобы добавить.")
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(cards) { card ->
                    MobileCardItem(card = card, viewModel = viewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AddCardDialog(viewModel = viewModel) { showAddDialog = false }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}
