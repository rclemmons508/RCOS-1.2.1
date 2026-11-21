package com.rcsolutions.rcosmobile.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.rcsolutions.rcosmobile.R
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.ChatMessage
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.CustomerRequest
import com.rcsolutions.rcosmobile.model.FieldTask
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.TeamMessage
import com.rcsolutions.rcosmobile.model.VoicemailRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RcosRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> observeMemberQuery(
        query: Query,
        path: String,
        uid: String,
        clazz: Class<T>,
        sorter: (List<T>) -> List<T> = { it }
    ): Flow<List<T>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            throw IllegalStateException("Authenticated user required.")
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    sorter(snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBusinesses(uid: String): Flow<List<BusinessAccount>> =
        observeMemberQuery(
            query = db.collection("businesses").whereArrayContains("memberUids", uid),
            path = "businesses",
            uid = uid,
            clazz = BusinessAccount::class.java
        ) { list ->
            list.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        }

    fun observeJobs(businessId: String, uid: String): Flow<List<Job>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("jobs").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/jobs",
            uid = uid,
            clazz = Job::class.java
        ) { list ->
            list.sortedBy { "${it.scheduledDate} ${it.scheduledTime}" }
        }

    fun observeCustomers(businessId: String, uid: String): Flow<List<Customer>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("customers").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/customers",
            uid = uid,
            clazz = Customer::class.java
        ) { list ->
            list.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        }

    fun observeTasks(businessId: String, uid: String): Flow<List<FieldTask>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("tasks").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/tasks",
            uid = uid,
            clazz = FieldTask::class.java
        ) { list ->
            list.sortedWith(compareBy<FieldTask> { it.completed }.thenByDescending { it.createdAt?.seconds ?: 0L })
        }

    fun observeCalls(businessId: String, uid: String): Flow<List<CallRecord>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("calls").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/calls",
            uid = uid,
            clazz = CallRecord::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeVoicemails(businessId: String, uid: String): Flow<List<VoicemailRecord>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("voicemails").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/voicemails",
            uid = uid,
            clazz = VoicemailRecord::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeRequests(businessId: String, uid: String): Flow<List<CustomerRequest>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("requests").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/requests",
            uid = uid,
            clazz = CustomerRequest::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeActions(businessId: String, uid: String): Flow<List<ActionRecord>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("actions").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/actions",
            uid = uid,
            clazz = ActionRecord::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeChatMessages(businessId: String, uid: String): Flow<List<ChatMessage>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("chatMessages").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/chatMessages",
            uid = uid,
            clazz = ChatMessage::class.java
        ) { list ->
            list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
        }

    fun observeTeamMessages(businessId: String, uid: String): Flow<List<TeamMessage>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("teamMessages").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/teamMessages",
            uid = uid,
            clazz = TeamMessage::class.java
        ) { list ->
            list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
        }

    fun observeAgentMessages(businessId: String, uid: String): Flow<List<AgentInboxMessage>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("agentMessages").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/agentMessages",
            uid = uid,
            clazz = AgentInboxMessage::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeAuditLogs(businessId: String, uid: String): Flow<List<AuditLog>> =
        observeMemberQuery(
            query = db.collection("businesses").document(businessId)
                .collection("auditLogs").whereArrayContains("memberUids", uid),
            path = "businesses/$businessId/auditLogs",
            uid = uid,
            clazz = AuditLog::class.java
        ) { list ->
            list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    // ---- Business Mutations ----
    suspend fun createBusiness(business: BusinessAccount): Result<String> {
        var path = "businesses"
        return try {
            val uid = requireUserId()
            require(business.name.trim().isNotEmpty()) { "Business name is required." }
            val ref = if (business.id.isNotBlank()) {
                db.collection("businesses").document(business.id)
            } else {
                db.collection("businesses").document()
            }
            path = ref.path
            val payload = business.toCreateMap(uid, auth.currentUser?.email)
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBusinessFields(
        businessId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val path = "businesses/$businessId"
        return try {
            requireUserId()
            require(businessId.isNotBlank()) { "Business ID required." }
            val safeUpdates = updates.toMutableMap().apply {
                remove("id")
                remove("ownerId")
                remove("createdAt")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("businesses").document(businessId).update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBusinessWorkspace(businessId: String): Result<Unit> {
        val path = "businesses/$businessId"
        return try {
            val uid = requireUserId()
            require(businessId.isNotBlank()) { "Business ID required." }
            val bizRef = db.collection("businesses").document(businessId)
            val subCollections = listOf(
                "requests", "actions", "jobs", "customers", "tasks",
                "calls", "voicemails", "chatMessages", "teamMessages",
                "agentMessages", "auditLogs"
            )
            for (sub in subCollections) {
                val docs = bizRef.collection(sub)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                docs.documents.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            bizRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Jobs / Schedule ----
    suspend fun createJob(business: BusinessAccount, job: Job): Result<String> {
        var path = "businesses/${business.id}/jobs"
        return try {
            val uid = requireUserId()
            require(job.title.trim().isNotEmpty()) { "Appointment title is required." }
            require(job.clientName.trim().isNotEmpty()) { "Customer name is required." }
            val ref = db.collection("businesses").document(business.id).collection("jobs").document()
            path = ref.path
            val bizOwner = business.ownerId.ifBlank { uid }
            val members = business.memberUids.ifEmpty { listOf(uid) }
            val payload = job.copy(businessId = business.id).toCreateMap(uid, bizOwner, members)
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateJobStatus(businessId: String, jobId: String, status: String, aiNotes: String? = null): Result<Unit> {
        val path = "businesses/$businessId/jobs/$jobId"
        return try {
            requireUserId()
            val map = buildMap<String, Any> {
                put("status", status)
                if (aiNotes != null) put("aiNotes", aiNotes.take(2000))
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.document(path).update(map).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteJob(businessId: String, jobId: String): Result<Unit> {
        val path = "businesses/$businessId/jobs/$jobId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Customers ----
    suspend fun createCustomer(business: BusinessAccount, customer: Customer): Result<String> {
        var path = "businesses/${business.id}/customers"
        return try {
            val uid = requireUserId()
            require(customer.name.trim().isNotEmpty()) { "Customer name is required." }
            val ref = db.collection("businesses").document(business.id).collection("customers").document()
            path = ref.path
            val payload = customer.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(businessId: String, customerId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "businesses/$businessId/customers/$customerId"
        return try {
            requireUserId()
            val safe = updates.toMutableMap().apply {
                remove("id")
                remove("businessId")
                remove("ownerId")
                remove("createdAt")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.document(path).update(safe).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(businessId: String, customerId: String): Result<Unit> {
        val path = "businesses/$businessId/customers/$customerId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Tasks ----
    suspend fun createTask(business: BusinessAccount, task: FieldTask): Result<String> {
        var path = "businesses/${business.id}/tasks"
        return try {
            val uid = requireUserId()
            require(task.title.trim().isNotEmpty()) { "Task title is required." }
            val ref = db.collection("businesses").document(business.id).collection("tasks").document()
            path = ref.path
            val payload = task.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(businessId: String, taskId: String, completed: Boolean): Result<Unit> {
        val path = "businesses/$businessId/tasks/$taskId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(businessId: String, taskId: String): Result<Unit> {
        val path = "businesses/$businessId/tasks/$taskId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Calls & Voicemails ----
    suspend fun createCallRecord(business: BusinessAccount, call: CallRecord): Result<String> {
        var path = "businesses/${business.id}/calls"
        return try {
            val uid = requireUserId()
            require(call.callerName.trim().isNotEmpty() && call.callerNumber.trim().isNotEmpty()) {
                "Caller name and phone number are required."
            }
            val ref = db.collection("businesses").document(business.id).collection("calls").document()
            path = ref.path
            val payload = call.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createVoicemail(business: BusinessAccount, vm: VoicemailRecord): Result<String> {
        var path = "businesses/${business.id}/voicemails"
        return try {
            val uid = requireUserId()
            val ref = db.collection("businesses").document(business.id).collection("voicemails").document()
            path = ref.path
            val payload = vm.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markVoicemailReviewed(businessId: String, voicemailId: String, reviewed: Boolean = true): Result<Unit> {
        val path = "businesses/$businessId/voicemails/$voicemailId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("reviewed" to reviewed, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---- Requests & Actions / Approvals ----
    suspend fun createRequest(business: BusinessAccount, request: CustomerRequest): Result<String> {
        var path = "businesses/${business.id}/requests"
        return try {
            val uid = requireUserId()
            require(request.customerName.trim().isNotEmpty()) { "Customer name is required." }
            val ref = db.collection("businesses").document(business.id).collection("requests").document()
            path = ref.path
            val payload = request.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createAction(business: BusinessAccount, action: ActionRecord): Result<String> {
        var path = "businesses/${business.id}/actions"
        return try {
            val uid = requireUserId()
            require(action.title.trim().isNotEmpty()) { "Action title is required." }
            val ref = db.collection("businesses").document(business.id).collection("actions").document()
            path = ref.path
            val payload = action.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun decideActionApproval(
        business: BusinessAccount,
        action: ActionRecord,
        decision: String, // "approved" or "rejected"
        editedDraft: String? = null,
        editedAmount: Double? = null,
        reviewerNote: String = ""
    ): Result<Unit> {
        val actionPath = "businesses/${business.id}/actions/${action.id}"
        return try {
            val uid = requireUserId()
            val ownerName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Owner"
            val bizOwner = business.ownerId.ifBlank { uid }
            val members = business.memberUids.ifEmpty { listOf(uid) }
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            db.runTransaction { tx ->
                val actionRef = db.document(actionPath)
                val auditRef = db.collection("businesses").document(business.id)
                    .collection("auditLogs").document()
                val shouldCreateJob = decision == ActionStatus.APPROVED.code && action.actionType == "schedule_job"
                val jobRef = if (shouldCreateJob) {
                    db.collection("businesses").document(business.id).collection("jobs").document()
                } else null

                val updates = buildMap<String, Any> {
                    put("status", decision)
                    put("decidedBy", ownerName.take(120))
                    if (editedDraft != null) put("draftContent", editedDraft.trim().take(4000))
                    if (editedAmount != null) put("dollarAmount", editedAmount.coerceIn(0.0, 1_000_000.0))
                    if (reviewerNote.isNotBlank()) put("reviewerNote", reviewerNote.trim().take(500))
                    if (jobRef != null) put("scheduledJobId", jobRef.id)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                tx.update(actionRef, updates)

                val auditPayload = buildMap<String, Any?> {
                    put("businessId", business.id)
                    put("ownerId", bizOwner)
                    put("memberUids", members)
                    put("actionId", action.id)
                    put("userId", uid)
                    put("actionType", action.actionType)
                    put("decision", decision)
                    put("summary", "${decision.replaceFirstChar { it.uppercase() }}: ${action.title}".take(500))
                    put("createdAt", FieldValue.serverTimestamp())
                }.filterValues { it != null } as Map<String, Any>
                tx.set(auditRef, auditPayload)

                if (jobRef != null) {
                    val jobPayload = Job(
                        businessId = business.id,
                        title = action.title.take(200),
                        clientName = action.clientName.ifBlank { "Client" },
                        estimatedValue = editedAmount ?: action.dollarAmount,
                        scheduledDate = todayIso,
                        scheduledTime = "10:00 AM",
                        category = business.services.firstOrNull() ?: "Service",
                        description = editedDraft ?: action.summary
                    ).toCreateMap(uid, bizOwner, members)
                    tx.set(jobRef, jobPayload)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, actionPath)
            Result.failure(e)
        }
    }

    suspend fun markInvoicePaid(businessId: String, actionId: String): Result<Unit> {
        val path = "businesses/$businessId/actions/$actionId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "paymentStatus" to "paid",
                    "status" to ActionStatus.COMPLETED.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---- Messages ----
    suspend fun saveChatMessage(business: BusinessAccount, msg: ChatMessage): Result<Unit> {
        var path = "businesses/${business.id}/chatMessages"
        return try {
            val uid = requireUserId()
            require(msg.text.trim().isNotEmpty()) { "Message cannot be blank." }
            val ref = db.collection("businesses").document(business.id).collection("chatMessages").document()
            path = ref.path
            val payload = msg.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTeamMessage(business: BusinessAccount, text: String): Result<Unit> {
        var path = "businesses/${business.id}/teamMessages"
        return try {
            val uid = requireUserId()
            val clean = text.trim()
            require(clean.isNotEmpty()) { "Team message cannot be blank." }
            val ref = db.collection("businesses").document(business.id).collection("teamMessages").document()
            path = ref.path
            val senderName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Owner"
            val payload = TeamMessage(
                businessId = business.id,
                senderId = uid,
                senderName = senderName,
                text = clean
            ).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAgentMessage(business: BusinessAccount, msg: AgentInboxMessage): Result<Unit> {
        var path = "businesses/${business.id}/agentMessages"
        return try {
            val uid = requireUserId()
            val ref = db.collection("businesses").document(business.id).collection("agentMessages").document()
            path = ref.path
            val payload = msg.copy(businessId = business.id).toCreateMap(
                uid,
                business.ownerId.ifBlank { uid },
                business.memberUids.ifEmpty { listOf(uid) }
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAgentMessageStatus(businessId: String, messageId: String, status: String): Result<Unit> {
        val path = "businesses/$businessId/agentMessages/$messageId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
