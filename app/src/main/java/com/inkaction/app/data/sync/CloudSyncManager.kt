package com.inkaction.app.data.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.inkaction.app.data.NoteRepository
import com.inkaction.app.data.SettingsRepository
import com.inkaction.app.data.crypto.CryptoManager
import com.inkaction.app.data.model.EncryptedCloudNote
import com.inkaction.app.data.model.SavedNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserAccountInfo(
    val email: String,
    val uid: String
)

sealed class SyncState {
    object Disabled : SyncState()
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val lastSyncTimestamp: Long, val message: String = "") : SyncState()
    data class Error(val message: String) : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _currentUserFlow = MutableStateFlow<UserAccountInfo?>(null)
    val currentUserFlow: StateFlow<UserAccountInfo?> = _currentUserFlow.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    init {
        updateSyncStateBasedOnSettings()
        checkCurrentUser()
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun ensureFirebase(): Boolean {
        return try {
            if (isFirebaseConfigured()) {
                if (firestore == null) firestore = FirebaseFirestore.getInstance()
                if (auth == null) auth = FirebaseAuth.getInstance()
                true
            } else {
                val settings = settingsRepository.settings.value
                if (settings.firebaseProjectId.isNotBlank() && settings.firebaseApiKey.isNotBlank()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("com.inkaction.app")
                        .setApiKey(settings.firebaseApiKey.trim())
                        .setProjectId(settings.firebaseProjectId.trim())
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    if (firestore == null) firestore = FirebaseFirestore.getInstance()
                    if (auth == null) auth = FirebaseAuth.getInstance()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun checkCurrentUser() {
        try {
            if (ensureFirebase()) {
                val current = auth?.currentUser
                if (current != null && !current.isAnonymous && !current.email.isNullOrBlank()) {
                    val info = UserAccountInfo(email = current.email ?: "", uid = current.uid)
                    _currentUserFlow.value = info
                    settingsRepository.setSyncAccountEmail(info.email)
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUserFlow.value = null
    }

    suspend fun signInWithEmail(email: String, password: String): Result<UserAccountInfo> = withContext(Dispatchers.IO) {
        if (!ensureFirebase()) {
            return@withContext Result.failure(Exception("Firebase není inicializován. Vložte soubor google-services.json nebo zadejte Firebase konfiguraci."))
        }
        try {
            val authInstance = auth ?: FirebaseAuth.getInstance()
            val authResult = authInstance.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("Přihlášení selhalo.")
            val info = UserAccountInfo(email = user.email ?: email.trim(), uid = user.uid)
            _currentUserFlow.value = info
            settingsRepository.setSyncAccountEmail(info.email)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<UserAccountInfo> = withContext(Dispatchers.IO) {
        if (!ensureFirebase()) {
            return@withContext Result.failure(Exception("Firebase není inicializován. Vložte soubor google-services.json nebo zadejte Firebase konfiguraci."))
        }
        try {
            val authInstance = auth ?: FirebaseAuth.getInstance()
            val authResult = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("Registrace selhala.")
            val info = UserAccountInfo(email = user.email ?: email.trim(), uid = user.uid)
            _currentUserFlow.value = info
            settingsRepository.setSyncAccountEmail(info.email)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<UserAccountInfo> = withContext(Dispatchers.IO) {
        if (!ensureFirebase()) {
            return@withContext Result.failure(Exception("Firebase není inicializován."))
        }
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authInstance = auth ?: FirebaseAuth.getInstance()
            val authResult = authInstance.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Přihlášení přes Google selhalo.")
            val info = UserAccountInfo(
                email = user.email ?: user.displayName ?: "Google User",
                uid = user.uid
            )
            _currentUserFlow.value = info
            settingsRepository.setSyncAccountEmail(info.email)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            _currentUserFlow.value = null
            settingsRepository.setSyncAccountEmail("")
            updateSyncStateBasedOnSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSyncStateBasedOnSettings() {
        val settings = settingsRepository.settings.value
        if (!settings.cloudSyncEnabled) {
            _syncState.value = SyncState.Disabled
        } else {
            val lastTime = settings.lastSyncTimestamp
            if (lastTime > 0) {
                _syncState.value = SyncState.Success(lastTime)
            } else {
                _syncState.value = SyncState.Idle
            }
        }
    }

    /**
     * Performs a bidirectional zero-knowledge E2EE sync between device and Firebase.
     * When cloud sync is disabled, this function exits immediately without network access.
     */
    suspend fun syncNow(customPassphrase: String? = null, forceEnable: Boolean = false): SyncState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val isEnabled = forceEnable || settings.cloudSyncEnabled
        if (!isEnabled) {
            val state = SyncState.Error("Synchronizace je vypnuta. Zapněte přepínač nahoře.")
            _syncState.value = state
            return@withContext state
        }

        if (!ensureFirebase()) {
            val state = SyncState.Error("Firebase není inicializován (chybí google-services.json nebo konfigurace)")
            _syncState.value = state
            return@withContext state
        }

        val currentAuth = auth ?: FirebaseAuth.getInstance()
        var user = currentAuth.currentUser
        if (user == null || user.email.isNullOrBlank()) {
            val state = SyncState.Error("Pro synchronizaci se nejprve přihlaste ke svému účtu níže.")
            _syncState.value = state
            return@withContext state
        }

        val passphrase = customPassphrase?.ifBlank { null }
            ?: settings.syncPassphrase.ifBlank { null }

        if (passphrase.isNullOrBlank()) {
            val state = SyncState.Error("Zadejte Sync Passphrase pro šifrování poznámek.")
            _syncState.value = state
            return@withContext state
        }

        val userId = user.uid
        _syncState.value = SyncState.Syncing

        try {
            val db = firestore ?: FirebaseFirestore.getInstance()
            val userNotesCollection = db.collection("users").document(userId).collection("notes")

            // 1. PUSH: Fetch local notes and encrypt those needing sync
            val localNotes = noteRepository.notesFlow.value
            for (localNote in localNotes) {
                val strokes = noteRepository.loadStrokes(localNote.id)
                val encryptedNote = CryptoManager.encryptNote(
                    note = localNote,
                    strokes = strokes,
                    passphrase = passphrase,
                    userId = userId
                )

                userNotesCollection.document(encryptedNote.id)
                    .set(encryptedNote, SetOptions.merge())
                    .await()
            }

            // 2. PULL: Fetch encrypted notes from cloud and decrypt
            val snapshot = userNotesCollection.get().await()
            val localNotesMap = localNotes.associateBy { it.id }.toMutableMap()

            var decryptionErrors = 0
            for (doc in snapshot.documents) {
                val cloudNote = doc.toObject(EncryptedCloudNote::class.java) ?: continue
                val noteId = cloudNote.id.toLongOrNull() ?: continue

                val existingLocal = localNotesMap[noteId]
                // If local note doesn't exist or cloud version is strictly newer:
                if (existingLocal == null || cloudNote.updatedAt > existingLocal.timestamp) {
                    try {
                        val (decryptedNote, decryptedStrokes) = CryptoManager.decryptNote(
                            cloudNote = cloudNote,
                            passphrase = passphrase
                        )

                        if (existingLocal == null) {
                            noteRepository.saveNote(
                                title = decryptedNote.title,
                                summary = decryptedNote.summary,
                                markdown = decryptedNote.markdown,
                                tags = decryptedNote.tags,
                                strokes = decryptedStrokes ?: emptyList()
                            )
                        } else {
                            noteRepository.updateNote(
                                noteId = noteId,
                                title = decryptedNote.title,
                                summary = decryptedNote.summary,
                                markdown = decryptedNote.markdown,
                                tags = decryptedNote.tags,
                                strokes = decryptedStrokes ?: emptyList()
                            )
                        }
                    } catch (e: Exception) {
                        decryptionErrors++
                        e.printStackTrace()
                    }
                }
            }

            val syncTime = System.currentTimeMillis()
            settingsRepository.setLastSyncTimestamp(syncTime)

            val successMsg = if (decryptionErrors > 0) {
                "Synchronizováno, ale $decryptionErrors poznámek se nepodařilo dešifrovat (zkontrolujte Passphrase)."
            } else {
                "Synchronizace úspěšně dokončena"
            }

            val successState = SyncState.Success(syncTime, successMsg)
            _syncState.value = successState
            return@withContext successState
        } catch (e: Exception) {
            e.printStackTrace()
            val errorState = SyncState.Error(e.localizedMessage ?: "Chyba při synchronizaci")
            _syncState.value = errorState
            return@withContext errorState
        }
    }
}
