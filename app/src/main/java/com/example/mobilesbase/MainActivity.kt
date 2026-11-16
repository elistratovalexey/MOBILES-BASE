package com.example.mobilesbase

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilesbase.data.MobileCard
import com.example.mobilesbase.utils.AppSettings
import com.example.mobilesbase.utils.ThemeMode
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

@Composable
fun ThemeOptionRow(label: String, mode: ThemeMode, current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(mode) }
    ) {
        RadioButton(selected = current == mode, onClick = { onChange(mode) })
        Text(label)
    }
}

@Composable
fun SettingsDialog(current: ThemeMode, onChange: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column {
                Text("Тема оформления", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ThemeOptionRow("Как в системе", ThemeMode.SYSTEM, current, onChange)
                ThemeOptionRow("Светлая", ThemeMode.LIGHT, current, onChange)
                ThemeOptionRow("Тёмная", ThemeMode.DARK, current, onChange)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Выбранная тема сохраняется и применяется сразу.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBalanceDialog(card: MobileCard, viewModel: MobileViewModel, onDismiss: () -> Unit) {
    var balanceText by remember { mutableStateOf(card.balance.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обновить баланс") },
        text = {
            Column {
                Text("Номер: ${card.phoneNumber}")
                Spacer(modifier = Modifier.height(8.dp))
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
        confirmButton = {
            Button(onClick = {
                viewModel.updateBalanceManually(card.id, balanceText.toDoubleOrNull() ?: 0.0)
                onDismiss()
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditDialog(viewModel: MobileViewModel, existingCard: MobileCard?, onDismiss: () -> Unit) {
    val isEdit = existingCard != null

    var phoneNumber by remember { mutableStateOf(existingCard?.phoneNumber ?: "+7 ") }
    var operator by remember { mutableStateOf(existingCard?.operator ?: "") }
    var payment by remember { mutableStateOf(existingCard?.monthlyPayment?.toString() ?: "") }
    var isUnlimitedMin by remember { mutableStateOf(existingCard?.monthlyMinutes == "Безлимит") }
    var minutes by remember {
        mutableStateOf(if (existingCard?.monthlyMinutes == "Безлимит") "" else (existingCard?.monthlyMinutes ?: ""))
    }
    var isUnlimitedGb by remember { mutableStateOf(existingCard?.monthlyGb == "Безлимит") }
    var gb by remember {
        mutableStateOf(if (existingCard?.monthlyGb == "Безлимит") "" else (existingCard?.monthlyGb ?: ""))
    }
    var simLocation by remember { mutableStateOf(existingCard?.simLocation ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать номер" else "Добавить номер") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Номер (+7 xxx xxx-xx-xx)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = operator,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Оператор") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OPERATORS_LIST.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { operator = option; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = payment,
                    onValueChange = { payment = it },
                    label = { Text("Оплата в месяц (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (isUnlimitedMin) "Безлимит" else minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Минуты") },
                        enabled = !isUnlimitedMin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isUnlimitedMin,
                        onCheckedChange = { isUnlimitedMin = it; if (it) minutes = "" }
                    )
                    Text("Безл.")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (isUnlimitedGb) "Безлимит" else gb,
                        onValueChange = { gb = it },
                        label = { Text("ГБ") },
                        enabled = !isUnlimitedGb,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isUnlimitedGb,
                        onCheckedChange = { isUnlimitedGb = it; if (it) gb = "" }
                    )
                    Text("Безл.")
                }
                OutlinedTextField(
                    value = simLocation,
                    onValueChange = { simLocation = it },
                    label = { Text("Где стоит SIM") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isEdit) {
                    Text(
                        "Баланс и дата его обновления сохраняются без изменений.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (phoneNumber.isNotBlank() && operator.isNotBlank()) {
                    val card = MobileCard(
                        id = existingCard?.id ?: 0,
                        phoneNumber = phoneNumber.trim(),
                        operator = operator,
                        monthlyPayment = payment.toDoubleOrNull() ?: 0.0,
                        monthlyMinutes = if (isUnlimitedMin) "Безлимит" else minutes.ifBlank { "0" },
                        monthlyGb = if (isUnlimitedGb) "Безлимит" else gb.ifBlank { "0" },
                        simLocation = simLocation.ifBlank { "Не указано" },
                        balance = existingCard?.balance ?: 0.0,
                        lastUpdated = existingCard?.lastUpdated ?: 0L
                    )
                    if (isEdit) viewModel.updateCard(card) else viewModel.insertCard(card)
                    onDismiss()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun MobileCardItem(card: MobileCard, viewModel: MobileViewModel, onEdit: () -> Unit) {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    val dateStr = if (card.lastUpdated > 0) {
        DateUtils.getRelativeTimeSpanString(card.lastUpdated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        "Никогда"
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.phoneNumber,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
                IconButton(onClick = { viewModel.deleteCard(card) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                text = card.operator,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "📍 ${card.simLocation}")
            Text(text = "💰 ${card.monthlyPayment} ₽/мес | 📞 ${card.monthlyMinutes} мин | 🌐 ${card.monthlyGb} ГБ")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Баланс: ${card.balance} ₽",
                style = MaterialTheme.typography.titleMedium,
                color = if (card.balance < 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Обновлен: $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val url = OPERATOR_URLS[card.operator]
                            ?: "https://google.com/search?q=${card.operator}+личный+кабинет"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Открыть ЛК")
                }
                OutlinedButton(onClick = { showUpdateDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("Ввести баланс")
                }
            }
        }
    }

    if (showUpdateDialog) {
        UpdateBalanceDialog(card = card, viewModel = viewModel) { showUpdateDialog = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncDialog(viewModel: MobileViewModel, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(SecureStorage.getEmail(context) ?: "") }
    var password by remember { mutableStateOf(SecureStorage.getPassword(context) ?: "") }
    var saveCreds by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var wasSaved by remember { mutableStateOf(SecureStorage.hasCredentials(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Синхронизация с Mail.ru") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Нужен ПАРОЛЬ ДЛЯ ВНЕШНЕГО ПРИЛОЖЕНИЯ: Mail.ru → Настройки → Безопасность → Пароли для внешних приложений.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (wasSaved) {
                    Text(
                        "✅ Данные уже сохранены на телефоне",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (например ivan@mail.ru)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль для внешнего приложения") },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
                    Text("Показать пароль")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveCreds, onCheckedChange = { saveCreds = it })
                    Text("Сохранить данные на телефоне")
                }
                if (wasSaved) {
                    TextButton(onClick = {
                        SecureStorage.clear(context)
                        email = ""
                        password = ""
                        wasSaved = false
                        onMessage("Данные удалены с телефона")
                    }) { Text("Забыть сохранённые данные") }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            onMessage("❌ Введите email и пароль для внешнего приложения")
                        } else {
                            if (saveCreds) {
                                viewModel.saveCloudCredentials(email, password)
                                wasSaved = true
                            }
                            viewModel.syncToCloud(onMessage)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Загрузить В облако")
                }
                TextButton(onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        onMessage("❌ Введите email и пароль для внешнего приложения")
                    } else {
                        if (saveCreds) {
                            viewModel.saveCloudCredentials(email, password)
                            wasSaved = true
                        }
                        viewModel.syncFromCloud(onMessage)
                        onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: MobileViewModel = viewModel()
) {
    val cards by viewModel.allCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<MobileCard?>(null) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои мобильные номера") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                    IconButton(onClick = { showCloudDialog = true }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Синхронизация")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет добавленных номеров. Нажмите +, чтобы добавить.")
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(cards) { card ->
                    MobileCardItem(
                        card = card,
                        viewModel = viewModel,
                        onEdit = { editingCard = card }
                    )
                }
            }
        }
        syncMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { syncMessage = null }) { Text("OK") } }
            ) { Text(msg) }
        }
    }

    if (showAddDialog) {
        CardEditDialog(viewModel = viewModel, existingCard = null) { showAddDialog = false }
    }
    editingCard?.let { card ->
        CardEditDialog(viewModel = viewModel, existingCard = card) { editingCard = null }
    }
    if (showSettingsDialog) {
        SettingsDialog(current = themeMode, onChange = onThemeChange) { showSettingsDialog = false }
    }
    if (showCloudDialog) {
        CloudSyncDialog(viewModel = viewModel, onDismiss = { showCloudDialog = false }, onMessage = { syncMessage = it })
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(AppSettings.getThemeMode(context)) }
            val useDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (useDark) darkColorScheme() else lightColorScheme()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        themeMode = themeMode,
                        onThemeChange = { newMode ->
                            themeMode = newMode
                            AppSettings.setThemeMode(context, newMode)
                        }
                    )
                }
            }
        }
    }
}
