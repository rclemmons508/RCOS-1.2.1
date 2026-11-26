package com.rcsolutions.rcosmobile.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.AiTaskProfile
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.ChatMessage
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.FieldTask
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.OnboardingExtraction
import com.rcsolutions.rcosmobile.model.QuoteDraftSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiRepository"
const val MODEL_DEEP_REASONING = "gemini-3.8-flash"
const val MODEL_COMPLEX_INTERACTIVE = "gemini-3.8-flash"
const val MODEL_GENERAL = "gemini-3.8-flash"
const val MODEL_FAST = "gemini-3.5-flash-lite"

data class StreamReplyEvent(
    val accumulatedText: String,
    val modelUsed: String
)

class AiRepository {

    private val onboardingModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "businessName" to Schema.string(description = "Detected company name"),
                "industryId" to Schema.enumeration(
                    values = RcosDefaults.INDUSTRY_PRESETS.map { it.id },
                    description = "Closest matching industry preset ID"
                ),
                "services" to Schema.array(
                    items = Schema.string(),
                    description = "Specific services offered by the business"
                ),
                "serviceArea" to Schema.string(description = "Cities, counties, or region served"),
                "businessHours" to Schema.string(description = "Operating hours"),
                "pricingApproach" to Schema.string(description = "Pricing model or rate notes"),
                "brandTone" to Schema.string(description = "Communication tone"),
                "emergencyAvailable" to Schema.boolean(description = "Whether emergency service is offered"),
                // Contact + team fields. Only what the document actually
                // states — absent means blank, never invented.
                "contactName" to Schema.string(description = "Business contact person's name, if stated"),
                "contactPhone" to Schema.string(description = "Business phone number, if stated"),
                "contactEmail" to Schema.string(description = "Business email address, if stated"),
                "teamSize" to Schema.string(description = "Team size or headcount, if stated (e.g. '1-5', '12 crew')"),
                "servesResidential" to Schema.boolean(description = "Whether residential customers are served, if stated"),
                "servesCommercial" to Schema.boolean(description = "Whether commercial customers are served, if stated")
            ),
            optionalProperties = listOf("serviceArea", "businessHours", "pricingApproach", "brandTone", "contactName", "contactPhone", "contactEmail", "teamSize", "servesResidential", "servesCommercial")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            systemInstruction = content {
                text("You extract structured service-business profiles accurately without inventing missing details.")
            },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val quoteModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Short quote title"),
                "summary" to Schema.string(description = "Concise scope of work summary"),
                "lineItemsSummary" to Schema.string(description = "Itemized pricing breakdown"),
                "totalAmount" to Schema.double(description = "Total estimated dollar amount"),
                "clientMessage" to Schema.string(description = "Warm professional quote message ready for owner approval")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_COMPLEX_INTERACTIVE,
            systemInstruction = content {
                text("You are an expert service-business estimator preparing itemized quotes ready for owner review.")
            },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val fastFollowUpModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            systemInstruction = content {
                text("You write concise, warm, ready-to-send customer follow-up messages for local service businesses.")
            }
        )
    }

    fun resolveModelName(
        employeeId: String,
        taskProfile: AiTaskProfile,
        hasMultimodalOrDoc: Boolean
    ): String {
        taskProfile.modelId?.let { return it }
        if (hasMultimodalOrDoc) return MODEL_COMPLEX_INTERACTIVE
        return when (employeeId) {
            "finance", "business_analyst", "systems_engineer", "project_manager" -> MODEL_DEEP_REASONING
            "sales", "operations", "technician" -> MODEL_COMPLEX_INTERACTIVE
            "ai_receptionist", "admin" -> MODEL_FAST
            else -> MODEL_GENERAL
        }
    }

    fun buildRoleSystemInstruction(
        business: BusinessAccount?,
        employeeId: String,
        customers: List<Customer>,
        jobs: List<Job>,
        tasks: List<FieldTask>,
        calls: List<CallRecord>,
        actions: List<ActionRecord>,
        isVoiceMode: Boolean = false
    ): String {
        val employee = RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }
            ?: RcosDefaults.AI_EMPLOYEES.first()
        val tone = business?.employeeTones?.get(employee.id) ?: employee.defaultTone
        val pendingApprovals = actions.count { it.status == "awaiting_approval" }
        return buildString {
            appendLine(employee.systemPromptRole)
            appendLine("Active Role: ${employee.name} (${employee.department}). Escalates to: ${employee.escalatesTo}.")
            appendLine("Voice & Communication Tone: $tone.")
            if (isVoiceMode) {
                appendLine("Speak naturally in short, clear conversational sentences suitable for real-time audio on a job site.")
            } else {
                appendLine("Respond clearly in plain, practical service-business language.")
            }
            if (business != null) {
                appendLine("Company: ${business.name} (Industry: ${RcosDefaults.findPreset(business.industry).name}).")
                if (business.services.isNotEmpty()) {
                    appendLine("Services: ${business.services.joinToString(", ")}.")
                }
                if (business.serviceArea.isNotBlank()) appendLine("Service area: ${business.serviceArea}.")
                if (business.businessHours.isNotBlank()) appendLine("Hours: ${business.businessHours}.")
                if (business.pricingApproach.isNotBlank()) appendLine("Pricing policy: ${business.pricingApproach}.")
                if (business.businessKnowledge.isNotBlank()) {
                    appendLine("Owner Knowledge Base:\n${business.businessKnowledge.take(3000)}")
                }
                appendLine("Safety & Approval Policy: Approval threshold is $${business.dollarThreshold.toInt()} (${business.autonomyMode} mode). Never execute external sends, quotes, or money commitments without owner confirmation.")
            }
            appendLine("Live Ops Snapshot: ${customers.size} customers, ${jobs.size} scheduled jobs, ${tasks.count { !it.completed }} open tasks, ${calls.size} calls logged, $pendingApprovals items awaiting approval.")
            if (customers.isNotEmpty()) {
                appendLine("Recent clients: ${customers.take(8).joinToString(", ") { "${it.name} (${it.status})" }}")
            }
            if (jobs.isNotEmpty()) {
                appendLine("Upcoming jobs: ${jobs.take(6).joinToString("; ") { "${it.title} for ${it.clientName} on ${it.scheduledDate} at ${it.scheduledTime}" }}")
            }
        }
    }

    fun streamAssistantReply(
        prompt: String,
        historyMessages: List<ChatMessage>,
        taskProfile: AiTaskProfile,
        business: BusinessAccount?,
        employeeId: String,
        customers: List<Customer>,
        jobs: List<Job>,
        tasks: List<FieldTask>,
        calls: List<CallRecord>,
        actions: List<ActionRecord>,
        attachedDocumentText: String = "",
        attachedBitmap: Bitmap? = null,
    ): Flow<StreamReplyEvent> = flow {
        val modelName = resolveModelName(
            employeeId = employeeId,
            taskProfile = taskProfile,
            hasMultimodalOrDoc = attachedBitmap != null || attachedDocumentText.isNotBlank()
        )
        val systemPrompt = buildRoleSystemInstruction(
            business = business,
            employeeId = employeeId,
            customers = customers,
            jobs = jobs,
            tasks = tasks,
            calls = calls,
            actions = actions,
            isVoiceMode = false
        )
        val model = Firebase.ai.generativeModel(
            modelName = modelName,
            systemInstruction = content { text(systemPrompt) }
        )
        val geminiHistory = buildGeminiChatHistory(historyMessages)
        val chat = model.startChat(history = geminiHistory)

        val requestContent = content {
            if (attachedDocumentText.isNotBlank()) {
                text("Attached Document Reference:\n${attachedDocumentText.take(4000)}")
            }
            if (attachedBitmap != null) {
                image(scaleBitmapUnderLimit(attachedBitmap))
            }
            text(prompt)
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(requestContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(StreamReplyEvent(accumulated.toString(), modelName))
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Assistant returned an empty response.")
        }
    }

    private fun buildGeminiChatHistory(messages: List<ChatMessage>): List<Content> {
        val recent = messages
            .filter { it.text.isNotBlank() }
            .takeLast(24)

        val normalized = mutableListOf<Pair<String, StringBuilder>>()
        for (msg in recent) {
            val role = if (msg.role == "user") "user" else "model"
            if (normalized.isEmpty() && role == "model") {
                // Gemini chat history must start with a user turn.
                continue
            }
            val last = normalized.lastOrNull()
            if (last != null && last.first == role) {
                last.second.append("\n").append(msg.text.trim())
            } else {
                normalized.add(role to StringBuilder(msg.text.trim()))
            }
        }
        // If the trailing persisted turn is an unanswered user message, drop it so startChat ends on a model turn
        // before chat.sendMessageStream sends the new user prompt.
        if (normalized.lastOrNull()?.first == "user") {
            normalized.removeAt(normalized.lastIndex)
        }
        return normalized.map { (role, textBuilder) ->
            content(role) { text(textBuilder.toString()) }
        }
    }

    suspend fun extractOnboardingProfile(
        descriptionOrDoc: String,
        attachedBitmaps: List<Bitmap> = emptyList()
    ): Result<OnboardingExtraction> = try {
        val response = withTimeout(60_000L) {
            onboardingModel.generateContent(
                content {
                    text(
                        "Extract the real service business profile from the text or image(s) provided. " +
                            "Do not invent prices or fields not present; provide clean industry classification and practical default services. " +
                            "Include the business contact name, phone, and email, team size, and whether residential and commercial customers are served — but only when the document actually states them; leave them blank when absent.\n\n" +
                            descriptionOrDoc.take(6000)
                    )
                    attachedBitmaps.take(6).forEach { bitmap ->
                        image(scaleBitmapUnderLimit(bitmap))
                    }
                }
            )
        }
        val raw = response.text ?: throw IllegalStateException("Model returned empty extraction.")
        val json = JSONObject(raw)
        val servicesArr = json.optJSONArray("services")
        val services = buildList {
            if (servicesArr != null) {
                for (i in 0 until servicesArr.length()) {
                    val s = servicesArr.optString(i).trim()
                    if (s.isNotEmpty()) add(s)
                }
            }
        }
        Result.success(
            OnboardingExtraction(
                businessName = json.optString("businessName", "").trim(),
                industryId = json.optString("industryId", "general_small_business").trim(),
                services = services,
                serviceArea = json.optString("serviceArea", "").trim(),
                businessHours = json.optString("businessHours", "").trim(),
                pricingApproach = json.optString("pricingApproach", "").trim(),
                brandTone = json.optString("brandTone", "").trim(),
                emergencyAvailable = json.optBoolean("emergencyAvailable", false),
                contactName = json.optString("contactName", "").trim(),
                contactPhone = json.optString("contactPhone", "").trim(),
                contactEmail = json.optString("contactEmail", "").trim(),
                teamSize = json.optString("teamSize", "").trim(),
                servesResidential = json.optBoolean("servesResidential", true),
                servesCommercial = json.optBoolean("servesCommercial", false)
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Onboarding extraction timed out", e)
        Result.failure(Exception("AI analysis timed out after 60 seconds. Please retry or select an industry manually."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Onboarding extraction failed", e)
        Result.failure(e)
    }

    suspend fun generateQuoteDraft(
        business: BusinessAccount?,
        clientName: String,
        serviceType: String,
        scopeNotes: String
    ): Result<QuoteDraftSuggestion> = try {
        val response = withTimeout(60_000L) {
            val prompt = buildString {
                appendLine("Prepare an itemized customer quote draft.")
                if (business != null) {
                    appendLine("Business: ${business.name} (${RcosDefaults.findPreset(business.industry).name})")
                    if (business.pricingApproach.isNotBlank()) appendLine("Pricing policy: ${business.pricingApproach}")
                    if (business.brandTone.isNotBlank()) appendLine("Brand tone: ${business.brandTone}")
                    if (business.businessKnowledge.isNotBlank()) appendLine("Reference rates/policies: ${business.businessKnowledge.take(2000)}")
                }
                appendLine("Customer: $clientName")
                appendLine("Service: $serviceType")
                if (scopeNotes.isNotBlank()) appendLine("Scope details: $scopeNotes")
            }
            quoteModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty quote response.")
        val json = JSONObject(raw)
        Result.success(
            QuoteDraftSuggestion(
                title = json.optString("title", "$serviceType Quote — $clientName"),
                summary = json.optString("summary", scopeNotes.ifBlank { serviceType }),
                lineItemsSummary = json.optString("lineItemsSummary", ""),
                totalAmount = json.optDouble("totalAmount", 0.0).coerceAtLeast(0.0),
                clientMessage = json.optString("clientMessage", "")
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Quote generation timed out", e)
        Result.failure(Exception("Quote generation timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Quote generation failed", e)
        Result.failure(e)
    }

    suspend fun generateCustomerFollowUp(
        business: BusinessAccount?,
        customer: Customer,
        purpose: String = "friendly check-in and service availability"
    ): Result<String> = try {
        val response = withTimeout(60_000L) {
            fastFollowUpModel.generateContent(
                buildString {
                    appendLine("Write a concise, ready-to-send customer follow-up message (SMS/Email length, 2-4 sentences).")
                    if (business != null) {
                        appendLine("From: ${business.name} (${business.brandTone}).")
                    }
                    appendLine("To customer: ${customer.name} (Status: ${customer.status}, Service: ${customer.serviceType.ifBlank { "service" }}).")
                    appendLine("Goal: $purpose")
                }
            )
        }
        val text = response.text?.trim()
        if (text.isNullOrBlank()) {
            Result.failure(IllegalStateException("AI returned an empty follow-up draft."))
        } else {
            Result.success(text)
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Follow-up generation timed out", e)
        Result.failure(Exception("Follow-up generation timed out. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Follow-up generation failed", e)
        Result.failure(e)
    }

    private fun scaleBitmapUnderLimit(bitmap: Bitmap): Bitmap {
        val maxDim = 1280
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}

/**
 * True when a Firebase AI failure is an App Check rejection (the
 * debug token was never registered in the console, or enforcement is
 * on for an unregistered build). The raw message ("Firebase App Check
 * token is invalid.") tells the owner nothing actionable, so callers
 * show the setup card instead — see AppCheckInit.readDebugSecret.
 */
fun isAppCheckFailure(error: Throwable?): Boolean {
    var current = error
    while (current != null) {
        if (current.message?.contains("app check", ignoreCase = true) == true) return true
        current = current.cause
    }
    return false
}

fun isAppCheckFailureMessage(message: String?): Boolean =
    message?.contains("app check", ignoreCase = true) == true
