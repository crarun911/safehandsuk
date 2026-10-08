package org.safeplan.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.safeplan.core.*
import org.safeplan.safety.*
import org.safeplan.support.SupportDirectory
import kotlin.random.Random

private val pages = listOf("Safety Check", "Personal Safety Plan", "Incident Journal", "Trusted People", "Find Support", "Privacy Centre")
private fun freshId(): String = (1..24).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")

@Composable
fun SafeplanApp(
    repository: PrivateRepository,
    pickPhoneContact: (((String, String) -> Unit) -> Unit)? = null
) {
    var page by remember { mutableStateOf("Home") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun runSafely(action: suspend () -> Unit) { scope.launch { try { error = null; action() } catch (_: Exception) { error = "Storage operation failed. Your changes may not have been saved." } } }
    Scaffold(topBar = { Surface(tonalElevation = 3.dp) { Text("SAFEPLAN · $page", Modifier.padding(18.dp), style = MaterialTheme.typography.titleLarge) } }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page != "Home") TextButton(onClick = { page = "Home" }) { Text("← Home") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (page) {
                "Home" -> {
                    Text("A safe place to work out what to do next.", style = MaterialTheme.typography.headlineSmall)
                    Text("Only use this app when it is safe to do so. A person with access to your unlocked device may see what you enter.")
                    pages.forEach { destination -> OutlinedButton(onClick = { page = destination }, modifier = Modifier.fillMaxWidth()) { Text(destination) } }
                }
                "Safety Check" -> SafetyScreen()
                "Personal Safety Plan" -> PlanScreen(repository, ::runSafely)
                "Incident Journal" -> JournalScreen(repository, ::runSafely)
                "Trusted People" -> ContactsScreen(repository, ::runSafely, pickPhoneContact)
                "Find Support" -> SupportScreen()
                "Privacy Centre" -> PrivacyScreen(repository, ::runSafely) { page = "Home" }
            }
        }
    }
}

@Composable private fun SafetyScreen() {
    var answer by remember { mutableStateOf<SafetyResponse?>(null) }
    Text("Are you in immediate danger?", style = MaterialTheme.typography.headlineSmall)
    Button(onClick = { answer = SafetyResponse.IMMEDIATE_DANGER }) { Text("Yes, I may be in danger") }
    OutlinedButton(onClick = { answer = SafetyResponse.NEED_GUIDANCE }) { Text("I'm unsure what to do") }
    OutlinedButton(onClick = { answer = SafetyResponse.SAFE_NOW }) { Text("I'm safe right now") }
    answer?.let { Text(guidance(it)); if (it == SafetyResponse.IMMEDIATE_DANGER) Text("In the UK, 999 is the emergency number. Do not delay seeking emergency help to use this app.") }
}

@Composable private fun PlanScreen(repo: PrivateRepository, run: (suspend () -> Unit) -> Unit) {
    var places by remember { mutableStateOf("") }; var items by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }; var people by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }
    LaunchedEffect(repo) { try { val plan = repo.getPlan(); places = plan.saferPlaces; items = plan.essentialItems; steps = plan.escalationSteps; people = plan.trustedPeople; loaded = true } catch (_: Exception) { message = "Could not load the plan" } }
    Text("Personal Safety Plan", style = MaterialTheme.typography.headlineSmall)
    if (!loaded) Text(message.ifBlank { "Loading…" })
    else {
        Field("Safer places", places) { places = it }
        Field("Essential items", items) { items = it }
        Field("Steps if the situation escalates", steps) { steps = it }
        Field("Trusted people and ways to reach them", people) { people = it }
        Button(onClick = { run { repo.savePlan(SafetyPlan(places, items, steps, people)); message = "Plan saved on this device" } }) { Text("Save plan") }
        if (message.isNotEmpty()) Text(message)
    }
}

@Composable private fun JournalScreen(repo: PrivateRepository, run: (suspend () -> Unit) -> Unit) {
    var entries by remember { mutableStateOf(emptyList<Incident>()) }
    var date by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }; var confirmDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repo) { try { entries = repo.incidents().sortedByDescending { it.whenText }; loaded = true } catch (_: Exception) { loaded = true } }
    Text("Private Incident Journal", style = MaterialTheme.typography.headlineSmall)
    Text("Enter the date and time yourself (for example, 2026-10-08 14:30). Entries are not independently verified.")
    Field("Date and time", date) { date = it }
    Field("What happened?", description) { description = it }
    Field("Location (optional)", location) { location = it }
    Button(enabled = date.isNotBlank() && description.isNotBlank(), onClick = {
        run { repo.addIncident(Incident(freshId(), date.trim(), description.trim(), location.trim())); entries = repo.incidents().sortedByDescending { it.whenText }; date = ""; description = ""; location = "" }
    }) { Text("Save incident") }
    HorizontalDivider()
    Text("Incident timeline", style = MaterialTheme.typography.titleLarge)
    if (loaded && entries.isEmpty()) Text("No incidents recorded")
    entries.forEach { entry -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(entry.whenText, style = MaterialTheme.typography.titleMedium)
        Text(entry.description); if (entry.location.isNotBlank()) Text("Location: ${entry.location}")
        if (confirmDelete == entry.id) {
            Text("Delete this entry permanently?")
            Row { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }; TextButton(onClick = { run { repo.deleteIncident(entry.id); entries = repo.incidents().sortedByDescending { it.whenText }; confirmDelete = null } }) { Text("Confirm delete") } }
        } else TextButton(onClick = { confirmDelete = entry.id }) { Text("Delete entry") }
    } } }
    Text("Attachments and evidence export are not supported in this build.", style = MaterialTheme.typography.bodySmall)
}

@Composable private fun ContactsScreen(
    repo: PrivateRepository,
    run: (suspend () -> Unit) -> Unit,
    pickPhoneContact: (((String, String) -> Unit) -> Unit)?
) {
    var contacts by remember { mutableStateOf(emptyList<TrustedContact>()) }
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var role by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repo) { try { contacts = repo.contacts() } catch (_: Exception) { } }
    Text("Trusted People", style = MaterialTheme.typography.headlineSmall)
    Text("Contacts are stored only within SAFEPLAN. No messages or calls are sent automatically.")
    OutlinedButton(
        onClick = {
            pickPhoneContact?.invoke { selectedName, selectedPhone ->
                name = selectedName
                phone = selectedPhone
                // Do not change role: users choose this within SAFEPLAN.
            }
        },
        enabled = pickPhoneContact != null,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Choose from phone contacts") }
    Text("Or enter their details manually below.")
    Field("Name", name) { name = it }
    Field("Phone number", phone) { phone = it }
    Field("Role (e.g. check-in contact)", role) { role = it }
    Button(enabled = name.isNotBlank(), onClick = { run { repo.addContact(TrustedContact(freshId(), name.trim(), phone.trim(), role.trim())); contacts = repo.contacts(); name = ""; phone = ""; role = "" } }) { Text("Add trusted person") }
    contacts.forEach { person -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text(person.name, style = MaterialTheme.typography.titleMedium); Text(person.phone); Text(person.role)
        if (confirmDelete == person.id) Row { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }; TextButton(onClick = { run { repo.deleteContact(person.id); contacts = repo.contacts(); confirmDelete = null } }) { Text("Confirm delete") } }
        else TextButton(onClick = { confirmDelete = person.id }) { Text("Delete") }
    } } }
}

@Composable private fun SupportScreen() {
    Text("Find UK Support", style = MaterialTheme.typography.headlineSmall)
    Text("Information is supplied offline. Verify current contact details independently when safe. In an emergency in the UK, call 999.")
    SupportDirectory.uk.forEach { service -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text(service.name, style = MaterialTheme.typography.titleMedium); Text(service.description); Text(service.website)
    } } }
}

@Composable private fun PrivacyScreen(repo: PrivateRepository, run: (suspend () -> Unit) -> Unit, goHome: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }; var result by remember { mutableStateOf("") }
    Text("Privacy Centre", style = MaterialTheme.typography.headlineSmall)
    Text("Plans, incidents and trusted people are stored locally in an encrypted app-private file on Android. The key is held in Android Keystore. No account, analytics, network permissions or automatic sharing are included.")
    Text("This does not protect against an unlocked or compromised device, screenshots taken by another device, or all forms of forensic recovery. Data export, attachments and account recovery are not implemented.")
    Text("Android backups and device-transfer data extraction are disabled for this app. Screen capture is blocked while the app is open.")
    if (confirm) {
        Text("Delete all SAFEPLAN entries and the encryption key from this device? This cannot be undone.")
        Row { TextButton(onClick = { confirm = false }) { Text("Cancel") }; Button(onClick = { run { repo.deleteAll(); confirm = false; result = "Local data deleted"; goHome() } }) { Text("Delete everything") } }
    } else OutlinedButton(onClick = { confirm = true }) { Text("Delete all my data") }
    if (result.isNotEmpty()) Text(result)
}

@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = if (label.contains("happened") || label.contains("escalates")) 3 else 1)
}
