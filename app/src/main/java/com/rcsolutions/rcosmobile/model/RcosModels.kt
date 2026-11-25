package com.rcsolutions.rcosmobile.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AutonomyMode(val code: String, val label: String) {
    SUPERVISED("supervised", "Supervised (Ask Approval)"),
    AUTONOMOUS("autonomous", "Autonomous (Auto-Execute Under Limit)");

    companion object {
        fun fromCode(raw: String?): AutonomyMode =
            entries.firstOrNull { it.code == raw } ?: SUPERVISED
    }
}

enum class ActionStatus(val code: String, val label: String) {
    QUEUED("queued", "Queued"),
    RUNNING("running", "Running"),
    AWAITING_APPROVAL("awaiting_approval", "Needs Approval"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Declined"),
    COMPLETED("completed", "Completed"),
    FAILED("failed", "Failed");

    companion object {
        fun fromCode(raw: String?): ActionStatus =
            entries.firstOrNull { it.code == raw } ?: AWAITING_APPROVAL
    }
}

enum class JobStatus(val code: String, val label: String) {
    UNASSIGNED("unassigned", "Unassigned"),
    DISPATCHED("dispatched", "Dispatched"),
    IN_PROGRESS("in_progress", "In Progress"),
    COMPLETED("completed", "Completed"),
    URGENT("urgent", "Urgent");

    companion object {
        fun fromCode(raw: String?): JobStatus =
            entries.firstOrNull { it.code == raw } ?: DISPATCHED
    }
}

enum class JobPriority(val code: String, val label: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High"),
    CRITICAL("critical", "Critical");

    companion object {
        fun fromCode(raw: String?): JobPriority =
            entries.firstOrNull { it.code == raw } ?: MEDIUM
    }
}

enum class CustomerStatus(val code: String, val label: String) {
    ACTIVE("active", "Active Customer"),
    LEAD("lead", "New Lead"),
    VIP("vip", "VIP Customer"),
    INACTIVE("inactive", "Archived");

    companion object {
        fun fromCode(raw: String?): CustomerStatus =
            entries.firstOrNull { it.code == raw } ?: ACTIVE
    }
}

enum class TaskBucket(val code: String, val label: String) {
    TODAY("today", "Today"),
    THIS_WEEK("this_week", "This Week");

    companion object {
        fun fromCode(raw: String?): TaskBucket =
            entries.firstOrNull { it.code == raw } ?: TODAY
    }
}

enum class TaskCategory(val code: String, val label: String) {
    CALL("call", "Call"),
    QUOTE("quote", "Quote"),
    SCHEDULE("schedule", "Schedule"),
    FOLLOW_UP("follow_up", "Follow Up"),
    JOB_CHECK("job_check", "Job Status");

    companion object {
        fun fromCode(raw: String?): TaskCategory =
            entries.firstOrNull { it.code == raw } ?: FOLLOW_UP
    }
}

enum class CallDirection(val code: String, val label: String) {
    INBOUND("inbound", "Incoming Call"),
    OUTBOUND("outbound", "Outgoing Call"),
    MISSED("missed", "Missed Call");

    companion object {
        fun fromCode(raw: String?): CallDirection =
            entries.firstOrNull { it.code == raw } ?: INBOUND
    }
}

enum class CallStatus(val code: String, val label: String) {
    ACTIVE("active", "Active"),
    COMPLETED("completed", "Completed"),
    MISSED("missed", "Missed"),
    VOICEMAIL("voicemail", "Voicemail"),
    TRANSFERRED("transferred", "Transferred");

    companion object {
        fun fromCode(raw: String?): CallStatus =
            entries.firstOrNull { it.code == raw } ?: COMPLETED
    }
}

@Keep
@IgnoreExtraProperties
data class BusinessAccount(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val memberEmails: Map<String, String> = emptyMap(),
    // Old-app roster shape (live docs): members[] of {userId, email, name, role}.
    // Kept in sync by toCreateMap so both apps can read the same business.
    val members: List<BusinessMember> = emptyList(),
    // Old-app contact block (live docs): {email, phone, name}. The live
    // business has NO top-level contactEmail — its email/phone live here.
    val documentContactInfo: DocumentContactInfo? = null,
    val name: String = "",
    val industry: String = "general_small_business",
    val size: String = "Solo / Small Crew",
    val services: List<String> = emptyList(),
    val pricingApproach: String = "Standard market rate with clear written quotes",
    val brandTone: String = "Professional, warm, dependable",
    val businessKnowledge: String = "",
    val painPoints: List<String> = emptyList(),
    val serviceArea: String = "",
    val businessHours: String = "Mon–Fri 8:00 AM – 6:00 PM",
    val emergencyAvailable: Boolean = false,
    val teamSize: String = "1-5",
    val servesResidential: Boolean = true,
    val servesCommercial: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val ownerEmail: String = "",
    val autonomyMode: String = AutonomyMode.SUPERVISED.code,
    val answeringMode: String = "ai",
    val aiReceptionistEnabled: Boolean = true,
    val dollarThreshold: Double = 250.0,
    val activeEmployees: Map<String, Boolean> = emptyMap(),
    val employeeTones: Map<String, String> = emptyMap(),
    val availableDepartments: List<String> = listOf("sales", "support", "billing", "dispatch", "emergency"),
    val onboardingCompleted: Boolean = false,
    val subscriptionTier: String = "Pro Plan",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, email: String?): Map<String, Any> {
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        val emailMap = if (cleanEmail.isNotEmpty()) mapOf(cleanEmail to "owner") else emptyMap()
        return buildMap<String, Any?> {
            put("ownerId", uid)
            put("memberUids", memberUids.ifEmpty { listOf(uid) }.distinct().take(25))
            put("memberEmails", emailMap.ifEmpty { memberEmails })
            // Dual-write the old app's roster shape so the business is
            // readable by the server / old app too: members[] + memberEmails.
            // A business written only with memberUids is invisible to the
            // old app; one written only with members is invisible to the
            // 1.2.1 memberUids queries. Write both, always.
            put("members", listOf(mapOf(
                "userId" to uid,
                "email" to cleanEmail,
                "name" to contactName.trim().ifEmpty { name.trim() },
                "role" to "owner"
            )))
            val contactInfo = buildMap<String, Any?> {
                put("email", contactEmail.trim().take(120).ifEmpty { null })
                put("phone", contactPhone.trim().take(30).ifEmpty { null })
                put("name", contactName.trim().take(100).ifEmpty { null })
            }.filterValues { it != null }
            if (contactInfo.isNotEmpty()) put("documentContactInfo", contactInfo)
            put("name", name.trim().take(100).ifEmpty { "My Business" })
            put("industry", industry.trim().take(60).ifEmpty { "general_small_business" })
            put("size", this@BusinessAccount.size.trim().take(50).ifEmpty { null })
            put("services", services.take(40))
            put("pricingApproach", pricingApproach.trim().take(150).ifEmpty { null })
            put("brandTone", brandTone.trim().take(150).ifEmpty { null })
            put("businessKnowledge", businessKnowledge.trim().take(8000).ifEmpty { null })
            put("painPoints", painPoints.take(20))
            put("serviceArea", serviceArea.trim().take(120).ifEmpty { null })
            put("businessHours", businessHours.trim().take(120).ifEmpty { null })
            put("emergencyAvailable", emergencyAvailable)
            put("teamSize", teamSize.trim().take(60).ifEmpty { null })
            put("servesResidential", servesResidential)
            put("servesCommercial", servesCommercial)
            put("contactName", contactName.trim().take(100).ifEmpty { null })
            put("contactPhone", contactPhone.trim().take(30).ifEmpty { null })
            put("contactEmail", contactEmail.trim().take(120).ifEmpty { null })
            put("ownerEmail", cleanEmail.take(120).ifEmpty { null })
            put("autonomyMode", AutonomyMode.fromCode(autonomyMode).code)
            put("answeringMode", if (answeringMode == "manual") "manual" else "ai")
            put("aiReceptionistEnabled", aiReceptionistEnabled)
            put("dollarThreshold", dollarThreshold.coerceIn(0.0, 1_000_000.0))
            put("activeEmployees", activeEmployees)
            put("employeeTones", employeeTones)
            put("availableDepartments", availableDepartments.take(10))
            put("onboardingCompleted", onboardingCompleted)
            put("subscriptionTier", subscriptionTier.trim().take(40).ifEmpty { "Pro Plan" })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
    }
}

// One entry of a business's members roster (old-app shape).
@Keep
@IgnoreExtraProperties
data class BusinessMember(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "member",
)

// Business contact block (old-app shape). The live business keeps its
// email/phone here, not in top-level contactEmail.
@Keep
@IgnoreExtraProperties
data class DocumentContactInfo(
    val email: String = "",
    val phone: String = "",
    val name: String = "",
)

// Per-user profile (top-level /users/{uid}). Phone + contact email live
// HERE, per person — never on the business doc — so a member saving
// their profile can never overwrite company contact info. Rules allow
// a user to read/write only their own doc.
@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val phone: String = "",
    val contactEmail: String = "",
    val businessIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, email: String?): Map<String, Any> =
        buildMap<String, Any?> {
            put("uid", uid)
            put("displayName", displayName.trim().take(100).ifEmpty { null })
            put("phone", phone.trim().take(30).ifEmpty { null })
            put("contactEmail", contactEmail.trim().lowercase()
                .ifEmpty { email?.trim()?.lowercase().orEmpty() }.take(120).ifEmpty { null })
            put("businessIds", businessIds.take(25))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> =
        buildMap<String, Any?> {
            put("displayName", displayName.trim().take(100))
            put("phone", phone.trim().take(30))
            put("contactEmail", contactEmail.trim().lowercase().take(120))
            put("businessIds", businessIds.take(25))
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

// Money loop: Quote -> Job -> Invoice -> Paid. No Stripe: invoices track
// Paid/Unpaid/Overdue status only.
@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val clientName: String = "",
    val clientId: String = "",
    val jobId: String = "",
    val quoteId: String = "",
    val amount: Double = 0.0,
    val status: String = "unpaid",
    val dueDate: String = "",
    val invoiceNumber: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("title", title.trim().take(200))
            put("clientName", clientName.trim().take(100))
            put("clientId", clientId.trim().take(128).ifEmpty { null })
            put("jobId", jobId.trim().take(128).ifEmpty { null })
            put("quoteId", quoteId.trim().take(128).ifEmpty { null })
            put("amount", amount.coerceIn(0.0, 1_000_000.0))
            put("status", if (status in listOf("draft", "unpaid", "paid", "overdue", "cancelled")) status else "unpaid")
            put("dueDate", dueDate.trim().take(40).ifEmpty { null })
            put("invoiceNumber", invoiceNumber.trim().take(40).ifEmpty { null })
            put("notes", notes.trim().take(2000).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Quote(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val clientName: String = "",
    val clientId: String = "",
    val jobId: String = "",
    val amount: Double = 0.0,
    val status: String = "draft",
    val lineItemsSummary: String = "",
    val clientMessage: String = "",
    val validUntil: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("title", title.trim().take(200))
            put("clientName", clientName.trim().take(100))
            put("clientId", clientId.trim().take(128).ifEmpty { null })
            put("jobId", jobId.trim().take(128).ifEmpty { null })
            put("amount", amount.coerceIn(0.0, 1_000_000.0))
            put("status", if (status in listOf("draft", "awaiting_approval", "approved", "rejected", "expired")) status else "draft")
            put("lineItemsSummary", lineItemsSummary.trim().take(4000).ifEmpty { null })
            put("clientMessage", clientMessage.trim().take(4000).ifEmpty { null })
            put("validUntil", validUntil.trim().take(40).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CustomerRequest(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val serviceLocation: String = "",
    val serviceType: String = "",
    val description: String = "",
    val urgency: String = "routine",
    val preferredDateTime: String = "",
    val stage: String = "Intake",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("customerName", customerName.trim().take(100))
            put("customerPhone", customerPhone.trim().take(30).ifEmpty { null })
            put("customerEmail", customerEmail.trim().take(120).ifEmpty { null })
            put("serviceLocation", serviceLocation.trim().take(200).ifEmpty { null })
            put("serviceType", serviceType.trim().take(100).ifEmpty { "General Service" })
            put("description", description.trim().take(2000).ifEmpty { null })
            put("urgency", urgency.takeIf { it in listOf("routine", "soon", "urgent", "emergency") } ?: "routine")
            put("preferredDateTime", preferredDateTime.trim().take(100).ifEmpty { null })
            put("stage", stage.trim().take(40).ifEmpty { "Intake" })
            put("status", status.takeIf { it in listOf("pending", "in_progress", "completed", "cancelled") } ?: "pending")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class ActionRecord(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val requestId: String = "",
    val employeeId: String = "sales",
    val actionType: String = "generate_quote",
    val title: String = "",
    val summary: String = "",
    val clientName: String = "",
    val status: String = ActionStatus.AWAITING_APPROVAL.code,
    val riskCategory: String = "commitment_outbound",
    val requiresApproval: Boolean = true,
    val dollarAmount: Double = 0.0,
    val draftContent: String = "",
    val invoiceNumber: String = "",
    val dueDate: String = "",
    val paymentStatus: String = "unpaid",
    val reviewerNote: String = "",
    val decidedBy: String = "",
    val scheduledJobId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("requestId", requestId.trim().take(128).ifEmpty { null })
            put("employeeId", employeeId.trim().take(50).ifEmpty { "sales" })
            put("actionType", actionType.trim().take(40).ifEmpty { "generate_quote" })
            put("title", title.trim().take(200))
            put("summary", summary.trim().take(1000).ifEmpty { null })
            put("clientName", clientName.trim().take(100).ifEmpty { null })
            put("status", ActionStatus.fromCode(status).code)
            put("riskCategory", riskCategory.trim().take(40).ifEmpty { "commitment_outbound" })
            put("requiresApproval", requiresApproval)
            put("dollarAmount", dollarAmount.coerceIn(0.0, 1_000_000.0))
            put("draftContent", draftContent.trim().take(4000).ifEmpty { null })
            put("invoiceNumber", invoiceNumber.trim().take(40).ifEmpty { null })
            put("dueDate", dueDate.trim().take(40).ifEmpty { null })
            put("paymentStatus", if (paymentStatus == "paid") "paid" else "unpaid")
            put("reviewerNote", reviewerNote.trim().take(500).ifEmpty { null })
            put("decidedBy", decidedBy.trim().take(120).ifEmpty { null })
            put("scheduledJobId", scheduledJobId.trim().take(128).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Job(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientId: String = "",
    val address: String = "",
    val status: String = JobStatus.DISPATCHED.code,
    val priority: String = JobPriority.MEDIUM.code,
    val assignedTechnician: String = "",
    val estimatedValue: Double = 0.0,
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val durationLabel: String = "1h",
    val category: String = "",
    val description: String = "",
    val aiNotes: String = "",
    val invoiceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("title", title.trim().take(200))
            put("clientName", clientName.trim().take(100))
            put("clientPhone", clientPhone.trim().take(30).ifEmpty { null })
            put("clientId", clientId.trim().take(128).ifEmpty { null })
            put("address", address.trim().take(200).ifEmpty { null })
            put("status", JobStatus.fromCode(status).code)
            put("priority", JobPriority.fromCode(priority).code)
            put("assignedTechnician", assignedTechnician.trim().take(100).ifEmpty { null })
            put("estimatedValue", estimatedValue.coerceIn(0.0, 1_000_000.0))
            put("scheduledDate", scheduledDate.trim().take(20))
            put("scheduledTime", scheduledTime.trim().take(40))
            put("durationLabel", durationLabel.trim().take(30).ifEmpty { null })
            put("category", category.trim().take(60).ifEmpty { "Service" })
            put("description", description.trim().take(2000).ifEmpty { null })
            put("aiNotes", aiNotes.trim().take(2000).ifEmpty { null })
            put("invoiceId", invoiceId.trim().take(128).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val status: String = CustomerStatus.ACTIVE.code,
    val healthScore: Long = 90L,
    val totalSpent: Double = 0.0,
    val activeJobsCount: Long = 0L,
    val lastContactDate: String = "",
    val serviceType: String = "",
    val aiSummary: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun initials(): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "RC"
        }
    }

    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("name", name.trim().take(100))
            put("company", company.trim().take(100).ifEmpty { null })
            put("phone", phone.trim().take(30).ifEmpty { null })
            put("email", email.trim().take(120).ifEmpty { null })
            put("address", address.trim().take(200).ifEmpty { null })
            put("status", CustomerStatus.fromCode(status).code)
            put("healthScore", healthScore.coerceIn(0L, 100L))
            put("totalSpent", totalSpent.coerceIn(0.0, 10_000_000.0))
            put("activeJobsCount", activeJobsCount.coerceIn(0L, 1000L))
            put("lastContactDate", lastContactDate.trim().take(40).ifEmpty { null })
            put("serviceType", serviceType.trim().take(100).ifEmpty { null })
            put("aiSummary", aiSummary.trim().take(1000).ifEmpty { null })
            put("tags", tags.take(15))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class FieldTask(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
    val dueTimeLabel: String = "",
    val bucket: String = TaskBucket.TODAY.code,
    val category: String = TaskCategory.FOLLOW_UP.code,
    val completed: Boolean = false,
    val relatedCustomerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("title", title.trim().take(140))
            put("subtitle", subtitle.trim().take(200).ifEmpty { null })
            put("dueTimeLabel", dueTimeLabel.trim().take(40).ifEmpty { null })
            put("bucket", TaskBucket.fromCode(bucket).code)
            put("category", TaskCategory.fromCode(category).code)
            put("completed", completed)
            put("relatedCustomerName", relatedCustomerName.trim().take(100).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val callerName: String = "",
    val callerNumber: String = "",
    val direction: String = CallDirection.INBOUND.code,
    val status: String = CallStatus.COMPLETED.code,
    val durationLabel: String = "0m",
    val timeLabel: String = "",
    val summary: String = "",
    val answeredBy: String = "ai_receptionist",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("callerName", callerName.trim().take(100))
            put("callerNumber", callerNumber.trim().take(30))
            put("direction", CallDirection.fromCode(direction).code)
            put("status", CallStatus.fromCode(status).code)
            put("durationLabel", durationLabel.trim().take(30).ifEmpty { null })
            put("timeLabel", timeLabel.trim().take(40).ifEmpty { null })
            put("summary", summary.trim().take(1000).ifEmpty { null })
            put("answeredBy", if (answeredBy == "human_operator") "human_operator" else "ai_receptionist")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class VoicemailRecord(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val callerName: String = "",
    val callerNumber: String = "",
    val durationLabel: String = "0:42",
    val transcription: String = "",
    val summary: String = "",
    val urgency: String = "medium",
    val reviewed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("callerName", callerName.trim().take(100))
            put("callerNumber", callerNumber.trim().take(30))
            put("durationLabel", durationLabel.trim().take(30).ifEmpty { null })
            put("transcription", transcription.trim().take(2000))
            put("summary", summary.trim().take(500).ifEmpty { null })
            put("urgency", urgency.takeIf { it in listOf("low", "medium", "high", "urgent") } ?: "medium")
            put("reviewed", reviewed)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val employeeId: String = "executive_assistant",
    val role: String = "user",
    val text: String = "",
    val modelUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("senderId", senderId.trim().take(128).ifEmpty { uid })
            put("senderName", senderName.trim().take(100).ifEmpty { null })
            put("employeeId", employeeId.trim().take(50).ifEmpty { "executive_assistant" })
            put("role", role.takeIf { it in listOf("user", "assistant", "model") } ?: "user")
            put("text", text.trim().take(8000))
            put("modelUsed", modelUsed.trim().take(80).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class TeamMessage(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("senderId", uid)
            put("senderName", senderName.trim().take(100).ifEmpty { "Owner" })
            put("text", text.trim().take(4000))
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class AgentInboxMessage(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val agentId: String = "executive_assistant",
    val kind: String = "suggestion",
    val title: String = "",
    val body: String = "",
    val status: String = "unread",
    val actionLabel: String = "",
    val actionRoute: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String, bizOwnerId: String, members: List<String>): Map<String, Any> =
        buildMap<String, Any?> {
            put("businessId", businessId)
            put("ownerId", bizOwnerId)
            put("memberUids", members.ifEmpty { listOf(uid) }.distinct().take(25))
            put("agentId", agentId.trim().take(50).ifEmpty { "executive_assistant" })
            put("kind", kind.takeIf { it in listOf("alert", "suggestion", "question") } ?: "suggestion")
            put("title", title.trim().take(160))
            put("body", body.trim().take(1500))
            put("status", status.takeIf { it in listOf("unread", "read", "answered", "dismissed") } ?: "unread")
            put("actionLabel", actionLabel.trim().take(60).ifEmpty { null })
            put("actionRoute", actionRoute.trim().take(40).ifEmpty { null })
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val actionId: String = "",
    val userId: String = "",
    val actionType: String = "",
    val decision: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class AIEmployee(
    val id: String,
    val name: String,
    val department: String,
    val defaultTone: String,
    val tagline: String,
    val coreJob: String,
    val escalatesTo: String,
    val systemPromptRole: String,
)

data class JobPackTemplate(
    val id: String,
    val serviceType: String,
    val checklist: List<String>,
    val preCheck: List<String>,
    val execute: List<String>,
    val cleanUp: List<String>,
    val clientConfirmation: List<String>,
    val requiredTools: List<String>,
    val safetyNotes: List<String>,
)

data class IndustryPreset(
    val id: String,
    val name: String,
    val description: String,
    val defaultServices: List<String>,
    val recommendedTone: String,
    val technicianRoleName: String,
    val suggestedEmployees: List<String>,
    val jobPack: JobPackTemplate,
)

data class AutomationRule(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean = true,
)

data class WeatherSnapshot(
    val temperatureFahrenheit: Int,
    val conditionLabel: String,
    val locationLabel: String,
    val isClear: Boolean,
)

data class OnboardingExtraction(
    val businessName: String = "",
    val industryId: String = "general_small_business",
    val services: List<String> = emptyList(),
    val serviceArea: String = "",
    val businessHours: String = "",
    val pricingApproach: String = "",
    val brandTone: String = "",
    val emergencyAvailable: Boolean = false,
)

data class QuoteDraftSuggestion(
    val title: String = "",
    val summary: String = "",
    val lineItemsSummary: String = "",
    val totalAmount: Double = 0.0,
    val clientMessage: String = "",
)

enum class AiTaskProfile(
    val id: String,
    val shortLabel: String,
    val modelId: String?,
    val description: String
) {
    AUTO("auto", "Auto", null, "Routes automatically by assistant role & task"),
    GENERAL("general", "3.8 Flash", "gemini-3.8-flash", "gemini-3.8-flash • General assistant tasks"),
    DEEP_REASONING("reasoning", "Reasoning", "gemini-3.8-flash", "gemini-3.8-flash • Deep reasoning & analysis"),
    COMPLEX_INTERACTIVE("interactive", "Multimodal", "gemini-3.8-flash", "gemini-3.8-flash • Docs, photos & interactive workflows"),
    FAST("fast", "Lite", "gemini-3.5-flash-lite", "gemini-3.5-flash-lite • Fast lightweight responses");
}

enum class LiveVoiceStatus {
    IDLE,
    CONNECTING,
    LIVE,
    ERROR
}

data class LiveVoiceUiState(
    val status: LiveVoiceStatus = LiveVoiceStatus.IDLE,
    val modelId: String = "gemini-3.8-live",
    val activeRoleName: String = "Executive Assistant",
    val durationSeconds: Int = 0,
    val statusDetail: String = "Tap Voice to start a real-time Gemini Live conversation",
    val error: String? = null
)

