package com.example.mobilesbase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions// <-- ДОБАВЛЕН ЭТОТ ИМПОРТ
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.viewmodel.MobileViewModel

val OPERATORS_LIST = listOf("МТС", "Билайн", "МегаФон", "Tele2", "Йота", "СберМобайл", "Тинькофф Мобайл", "Ростелеком")
val OPERATOR_URLS = mapOf(
    "МТС" to "https://moskva.mts.ru/personal",
    "Билайн" to "https://my.beeline.ru/",
    "МегаФон" to "https://lk.megafon.ru/",
    "Tele2" to "https://msk.tele2.ru/",
    "Йота" to "https://my.yota.ru/",
    "СберМобайл" to "https://sbermobile.ru/",
    "Тинькофф Мобайл" to "https://www.tinkoff.ru/mobile/",
    "Ростелеком" to "https://lk.rt.ru/"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBalanceDialog(card: MobileCard, viewModel: MobileViewModel, onDismiss: () -> Unit) {
    var balanceText by remember { mutableStateOf(card.balance.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Обновить баланс для ${card.phoneNumber}") },
        text = {
            Column {
                Text("Откройте личный кабинет, посмотрите баланс и введите его сюда:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = balanceText, 
                    onValueChange = { balanceText = it }, 
                    label = { Text("Баланс (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { viewModel.updateBalanceManually(card.id, balanceText.toDoubleOrNull() ?: 0.0); onDismiss() }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

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
        onDismissRequest = onDismiss, title = { Text("Добавить номер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Номер (+7...)") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = operator, onValueChange = {}, readOnly = true, label = { Text("Оператор") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OPERATORS_LIST.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { operator = option; expanded = false }) }
                    }
                }
                OutlinedTextField(value = payment, onValueChange = { payment = it }, label = { Text("Оплата в месяц (₽)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = if (isUnlimitedMin) "Безлимит" else minutes, onValueChange = { minutes = it }, label = { Text("Минуты") }, enabled = !isUnlimitedMin, modifier = Modifier.weight(1f))
                    Checkbox(checked = isUnlimitedMin, onCheckedChange = { isUnlimitedMin = it; if (it) minutes = "Безлимит" else minutes = "" })
                    Text("Безл.")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = if (isUnlimitedGb) "Безлимит" else gb, onValueChange = { gb = it }, label = { Text("ГБ") }, enabled = !isUnlimitedGb, modifier = Modifier.weight(1f))
                    Checkbox(checked = isUnlimitedGb, onCheckedChange = { isUnlimitedGb = it; if (it) gb = "Безлимит" else gb = "" })
                    Text("Безл.")
                }
                OutlinedTextField(value = simLocation, onValueChange = { simLocation = it }, label = { Text("Где стоит SIM") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (phoneNumber.isNotBlank() && operator.isNotBlank()) {
                    viewModel.insertCard(MobileCard(phoneNumber = phoneNumber.trim(), operator = operator, monthlyPayment = payment.toDoubleOrNull() ?: 0.0,
                        monthlyMinutes = if (isUnlimitedMin) "Безлимит" else (minutes.ifBlank { "0" }), monthlyGb = if (isUnlimitedGb) "Безлимит" else (gb.ifBlank { "0" }),
                        simLocation = simLocation.ifBlank { "Не указано" }, balance = 0.0, lastUpdated = 0L))
                    onDismiss()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun MobileCardItem(card: MobileCard, viewModel: MobileViewModel) {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    val dateStr = if (card.lastUpdated > 0) DateUtils.getRelativeTimeSpanString(card.lastUpdated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString() else "Никогда"

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = card.phoneNumber, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.deleteCard(card) }) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) }
            }
            Text(text = card.operator, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "📍 ${card.simLocation}")
            Text(text = "💰 ${card.monthlyPayment} ₽/мес | 📞 ${card.monthlyMinutes} мин | 🌐 ${card.monthlyGb} ГБ")
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Баланс: ${card.balance} ₽", style = MaterialTheme.typography.titleMedium, color = if (card.balance < 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text(text = "Обновлен: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val url = OPERATOR_URLS[card.operator] ?: "https://google.com/search?q=${card.operator}+личный+кабинет"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Открыть ЛК")
                }
                OutlinedButton(onClick = { showUpdateDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ввести баланс")
                }
            }
        }
    }
    if (showUpdateDialog) UpdateBalanceDialog(card = card, viewModel = viewModel) { showUpdateDialog = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MobileViewModel = viewModel()) {
    val cards by viewModel.allCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Мои мобильные номера") }, actions = {
                IconButton(onClick = { showCloudDialog = true }) { Icon(Icons.Default.CloudUpload, contentDescription = "Синхронизация") }
            })
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Добавить") } }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Нет добавленных номеров. Нажмите +, чтобы добавить.") }
        } else {
            LazyColumn(contentPadding = padding) { items(cards) { card -> MobileCardItem(card = card, viewModel = viewModel) } }
        }
        syncMessage?.let { msg ->
            Snackbar(modifier = Modifier.padding(16.dp), action = { TextButton(onClick = { syncMessage = null }) { Text("OK") } }) { Text(msg) }
        }
    }
    if (showAddDialog) AddCardDialog(viewModel = viewModel) { showAddDialog = false }
    if (showCloudDialog) CloudSyncDialog(viewModel = viewModel, onDismiss = { showCloudDialog = false }, onMessage = { syncMessage = it })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncDialog(viewModel: MobileViewModel, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSavingCreds by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Синхронизация с Mail.ru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Пароль должен быть 'Паролем для внешних приложений' из настроек безопасности Mail.ru!")
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (test@mail.ru)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль для внешних приложений") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSavingCreds, onCheckedChange = { isSavingCreds = it })
                    Text("Сохранить данные в памяти телефона")
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        if (isSavingCreds) viewModel.saveCloudCredentials(email, password)
                        viewModel.syncToCloud(onMessage)
                        onDismiss()
                    }
                }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Загрузить В облако")
                }
                TextButton(onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        if (isSavingCreds) viewModel.saveCloudCredentials(email, password)
                        viewModel.syncFromCloud(onMessage)
                        onDismiss()
                    } else {
                        onMessage("❌ Введите Email и пароль")
                    }
                }) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Восстановить ИЗ облака")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) { MainScreen() }
            }
        }
    }
}
