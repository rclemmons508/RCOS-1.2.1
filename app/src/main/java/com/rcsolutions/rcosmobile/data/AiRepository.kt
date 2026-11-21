package com.rcsolutions.rcosmobile.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallRecord
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
private const val MODEL_PRIMARY = "gemini-3.8-flash"
private const val MODEL_FAST = "gemini-3.5-flash-lite"

class AiRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_PRIMARY)
    }

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
                "emergencyAvailable" to Schema.boolean(description = "Whether emergency service is offered")
            ),
            optionalProperties = listOf("serviceArea", "businessHours", "pricingApproach", "brandTone")
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
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
            modelName = MODEL_PRIMARY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun streamAssistantReply(
        prompt: String,
        business: BusinessAccount?,
        employeeId: String,
        customers: List<Customer>,
        jobs: List<Job>,
        tasks: List<FieldTask>,
        calls: List<CallRecord>,
        actions: List<ActionRecord>,
        attachedDocumentText: String = "",
        attachedBitmap: Bitmap? = null,
    ): Flow<String> = flow {
        val employee = RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }
            ?: RcosDefaults.AI_EMPLOYEES.first()
        val tone = business?.employeeTones?.get(employee.id) ?: employee.defaultTone
        val pendingApprovals = actions.count { it.status == "awaiting_approval" }
        val contextSummary = buildString {
            appendLine(employee.systemPromptRole)
            appendLine("Voice tone: $tone.")
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
                appendLine("Safety rule: Approval threshold is $${business.dollarThreshold.toInt()} (${business.autonomyMode} mode). Never execute external sends or money commitments without owner confirmation.")
            }
            appendLine("Live Ops Snapshot: ${customers.size} customers, ${jobs.size} scheduled jobs, ${tasks.count { !it.completed }} open tasks, ${calls.size} calls logged, $pendingApprovals items awaiting approval.")
            if (customers.isNotEmpty()) {
                appendLine("Recent clients: ${customers.take(8).joinToString(", ") { "${it.name} (${it.status})" }}")
            }
            if (jobs.isNotEmpty()) {
                appendLine("Upcoming jobs: ${jobs.take(6).joinToString("; ") { "${it.title} for ${it.clientName} on ${it.scheduledDate} at ${it.scheduledTime}" }}")
            }
            if (attachedDocumentText.isNotBlank()) {
                appendLine("Attached Document Reference:\n${attachedDocumentText.take(4000)}")
            }
            appendLine("\nRespond clearly in plain, practical service-business language.")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            val requestContent = content {
                text(contextSummary)
                if (attachedBitmap != null) {
                    image(scaleBitmapUnderLimit(attachedBitmap))
                }
                text("Owner request: $prompt")
            }
            chatModel.generateContentStream(requestContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Assistant returned an empty response.")
        }
    }

    suspend fun extractOnboardingProfile(
        descriptionOrDoc: String,
        attachedBitmap: Bitmap? = null
    ): Result<OnboardingExtraction> = try {
        val response = withTimeout(60_000L) {
            onboardingModel.generateContent(
                content {
                    text(
                        "Extract the real service business profile from the text or image provided. " +
                            "Do not invent prices or fields not present; provide clean industry classification and practical default services.\n\n" +
                            descriptionOrDoc.take(6000)
                    )
                    if (attachedBitmap != null) {
                        image(scaleBitmapUnderLimit(attachedBitmap))
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
                emergencyAvailable = json.optBoolean("emergencyAvailable", false)
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
            chatModel.generateContent(
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
