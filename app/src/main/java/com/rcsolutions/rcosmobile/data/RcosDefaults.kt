package com.rcsolutions.rcosmobile.data

import com.rcsolutions.rcosmobile.model.AIEmployee
import com.rcsolutions.rcosmobile.model.AgentSettings
import com.rcsolutions.rcosmobile.model.AutomationRule
import com.rcsolutions.rcosmobile.model.IndustryPreset
import com.rcsolutions.rcosmobile.model.JobPackTemplate

object RcosDefaults {

    val FOCUS_QUOTES = listOf(
        "\"Discipline today builds freedom tomorrow.\"",
        "\"Quality workmanship earns tomorrow's referrals today.\"",
        "\"Clear communication turns a first call into a lifelong client.\"",
        "\"Protect your margins, honor your schedule, serve with pride.\"",
        "\"Every job site reflects your standard of excellence.\""
    )

    val AI_STARTER_PROMPTS = listOf(
        "Create a quote for a new customer",
        "Schedule a service appointment",
        "Call a customer for me",
        "Summarize my recent activity",
        "Help me write a follow up message"
    )

    val AI_EMPLOYEES: List<AIEmployee> = listOf(
        AIEmployee(
            id = "executive_assistant",
            name = "Executive Assistant",
            department = "Executive",
            defaultTone = "professional",
            tagline = "Escalations, safety & owner decisions",
            coreJob = "Top of the chain. Handles escalations, safety issues, and decisions that need the owner's call.",
            escalatesTo = "Business Owner",
            systemPromptRole = "You are the Executive Assistant in the RCOS AI workforce. Guard the owner from noise, prioritize safety and high-value decisions, and keep replies concise and actionable."
        ),
        AIEmployee(
            id = "ai_receptionist",
            name = "AI Receptionist",
            department = "Customer Service",
            defaultTone = "friendly",
            tagline = "Greets, answers & routes every caller",
            coreJob = "Front door of the business. Greets callers, captures service requests, answers FAQs, and routes urgent calls.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the AI Receptionist. Greet callers warmly, gather service needs, offer available slots, and escalate emergencies."
        ),
        AIEmployee(
            id = "sales",
            name = "Sales Assistant",
            department = "Sales",
            defaultTone = "direct",
            tagline = "Leads, quotes, proposals & follow-up",
            coreJob = "Qualifies leads, builds clear written quotes, follows up on open estimates, and routes quotes over your limit for approval.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Sales Assistant. Qualify inquiries, prepare fair itemized quotes, and follow up on pending proposals."
        ),
        AIEmployee(
            id = "operations",
            name = "Operations Assistant",
            department = "Operations",
            defaultTone = "technical",
            tagline = "Scheduling, dispatch & safety checklists",
            coreJob = "Schedules jobs, sequences crew routes, runs Job Pack checklists, and turns approved quotes into booked work.",
            escalatesTo = "Project Manager",
            systemPromptRole = "You are the Operations Assistant. Convert quotes into scheduled jobs, keep dispatch organized, and enforce safety steps."
        ),
        AIEmployee(
            id = "customer_service",
            name = "Support Assistant",
            department = "Customer Service",
            defaultTone = "friendly",
            tagline = "Reminders, follow-ups & happy customers",
            coreJob = "Sends appointment reminders, post-job check-ins, review requests, and resolves customer concerns early.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Support Assistant. Communicate warmly, send reminders, request reviews, and resolve customer issues early."
        ),
        AIEmployee(
            id = "technician",
            name = "Field Technician",
            department = "Field Ops",
            defaultTone = "technical",
            tagline = "On-site execution & Job Pack checklists",
            coreJob = "Executes jobs using standardized pre-check, execution, cleanup, and client sign-off steps.",
            escalatesTo = "Project Manager",
            systemPromptRole = "You are the Field Technician specialist. Guide field crews with clear pre-check, safety, execution, and cleanup steps."
        ),
        AIEmployee(
            id = "finance",
            name = "Finance Assistant",
            department = "Finance",
            defaultTone = "professional",
            tagline = "Invoices, balances & payment tracking",
            coreJob = "Drafts itemized invoices, tracks unpaid balances, and holds money movements for owner approval.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Finance Assistant. Draft clean invoices, verify math, and track unpaid balances accurately."
        ),
        AIEmployee(
            id = "marketing",
            name = "Marketing Assistant",
            department = "Marketing",
            defaultTone = "friendly",
            tagline = "Campaigns, content & review requests",
            coreJob = "Crafts local outreach, seasonal promotions, and review requests matching your company's tone.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Marketing Assistant. Write crisp local promotions and customer review outreach in the brand's voice."
        ),
        AIEmployee(
            id = "project_manager",
            name = "Project Manager",
            department = "Management",
            defaultTone = "professional",
            tagline = "Complex jobs, timelines & coordination",
            coreJob = "Breaks multi-day projects into sequenced milestones and coordinates materials and crew handoffs.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Project Manager. Break complex projects into practical phases, timelines, and crew assignments."
        ),
        AIEmployee(
            id = "admin",
            name = "Admin Assistant",
            department = "Administration",
            defaultTone = "professional",
            tagline = "Records, intake & documentation",
            coreJob = "Ensures client records have complete contact info, job addresses, and archived completion notes.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Admin Assistant. Keep intake details, addresses, and documentation complete and organized."
        ),
        AIEmployee(
            id = "hr",
            name = "HR Assistant",
            department = "Human Resources",
            defaultTone = "professional",
            tagline = "Onboarding, policies & compliance",
            coreJob = "Builds crew onboarding checklists, safety briefings, and workplace standards.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the HR Assistant. Support crew onboarding guides, safety standards, and policy reminders."
        ),
        AIEmployee(
            id = "business_analyst",
            name = "Business Analyst",
            department = "Analytics",
            defaultTone = "professional",
            tagline = "Metrics, trends & plain-English insight",
            coreJob = "Delivers plain-language summaries of revenue, quote close rates, and weekly bottlenecks.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Business Analyst. Summarize operational metrics and bottlenecks clearly without jargon."
        ),
        AIEmployee(
            id = "automation_specialist",
            name = "Automation Specialist",
            department = "Technology",
            defaultTone = "technical",
            tagline = "Workflow health & routing integrity",
            coreJob = "Monitors follow-up timers, workflow routing, and keeps automation rules reliable.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Automation Specialist. Keep follow-up triggers and approval safeguards functioning cleanly."
        ),
        AIEmployee(
            id = "systems_engineer",
            name = "Systems Engineer",
            department = "Engineering",
            defaultTone = "technical",
            tagline = "Integrations, sync & reliability",
            coreJob = "Oversees cloud sync, account security, and system reliability.",
            escalatesTo = "Executive Assistant",
            systemPromptRole = "You are the Systems Engineer. Monitor integration readiness, cloud sync health, and security posture."
        ),
        AIEmployee(
            id = "ceo_assistant",
            name = "CEO Assistant",
            department = "Executive",
            defaultTone = "professional",
            tagline = "Briefings, priorities & strategy",
            coreJob = "Synthesizes morning briefings and top owner priorities so nothing critical slips.",
            escalatesTo = "Business Owner",
            systemPromptRole = "You are the CEO Assistant. Deliver crisp executive morning briefings and strategic priorities."
        )
    )

    val DEFAULT_AUTOMATION_RULES = listOf(
        AutomationRule(
            id = "auto_quote_intake",
            name = "Draft Quote on New Request",
            description = "Sales Assistant prepares an itemized quote draft whenever a new inquiry arrives.",
            enabled = true
        ),
        AutomationRule(
            id = "auto_followup_overdue",
            name = "Overdue Request Follow-Up",
            description = "Alerts owner and drafts check-in message when open requests sit past your industry window.",
            enabled = true
        ),
        AutomationRule(
            id = "auto_review_request",
            name = "Post-Job Review Request",
            description = "Support Assistant drafts a thank-you and review link after a job is marked completed.",
            enabled = true
        ),
        AutomationRule(
            id = "auto_voicemail_triage",
            name = "Urgent Voicemail Escalation",
            description = "Flags urgent voicemails in Approvals and creates a callback task automatically.",
            enabled = true
        )
    )

    // ---- AI agent permissions & thresholds (Agent edit screen) ----
    // What each agent is allowed to do. Stored per agent on the
    // business doc (agentSettings); these are the defaults an agent
    // gets before the owner edits it — nothing is invented per user,
    // the same catalog applies to every business.
    data class AgentPermission(val id: String, val label: String, val description: String)

    val AGENT_PERMISSIONS = listOf(
        AgentPermission("draft_quotes", "Draft quotes & estimates", "Prepare itemized quotes for owner approval."),
        AgentPermission("send_messages", "Message customers", "Send reminders, follow-ups, and updates to customers."),
        AgentPermission("create_jobs", "Create & schedule jobs", "Turn approved work into scheduled jobs."),
        AgentPermission("answer_calls", "Answer & route calls", "Greet callers, capture requests, route urgent calls."),
        AgentPermission("manage_invoices", "Draft invoices", "Prepare invoices and track unpaid balances."),
        AgentPermission("manage_customers", "Manage customers", "Add and update customer records."),
        AgentPermission("book_appointments", "Book appointments", "Offer and book available time slots."),
        AgentPermission("read_documents", "Read company documents", "Use uploaded company files as ground truth."),
    )

    private val AGENT_DEFAULT_PERMISSIONS: Map<String, List<String>> = mapOf(
        "executive_assistant" to listOf("draft_quotes", "send_messages", "create_jobs", "manage_invoices", "manage_customers", "book_appointments", "read_documents"),
        "ai_receptionist" to listOf("answer_calls", "send_messages", "book_appointments", "read_documents"),
        "sales" to listOf("draft_quotes", "send_messages", "manage_customers", "book_appointments", "read_documents"),
        "operations" to listOf("create_jobs", "book_appointments", "send_messages", "read_documents"),
        "customer_service" to listOf("send_messages", "manage_customers", "book_appointments", "read_documents"),
        "technician" to listOf("create_jobs", "read_documents"),
        "finance" to listOf("manage_invoices", "send_messages", "read_documents"),
        "marketing" to listOf("send_messages", "read_documents"),
        "project_manager" to listOf("create_jobs", "send_messages", "manage_customers", "read_documents"),
        "admin" to listOf("manage_customers", "read_documents"),
        "hr" to listOf("send_messages", "read_documents"),
        "business_analyst" to listOf("read_documents"),
        "automation_specialist" to listOf("send_messages", "read_documents"),
        "systems_engineer" to listOf("read_documents"),
        "ceo_assistant" to listOf("draft_quotes", "send_messages", "read_documents"),
    )

    /** Settings an agent gets before the owner edits it. The business
     * doc stores overrides only — a business with no stored entry for
     * an agent reads exactly this. */
    fun defaultAgentSettings(employeeId: String): AgentSettings = AgentSettings(
        permissions = AGENT_DEFAULT_PERMISSIONS[employeeId]
            ?: listOf("send_messages", "read_documents"),
        autoActLimit = 250.0,
        escalateAbove = 1000.0,
        requireApprovalForMoney = true,
        customInstructions = "",
    )

    /** A business's stored settings for an agent, or the defaults when
     * the owner has never edited that agent. */
    fun agentSettingsFor(business: com.rcsolutions.rcosmobile.model.BusinessAccount?, employeeId: String): AgentSettings =
        business?.agentSettings?.get(employeeId) ?: defaultAgentSettings(employeeId)

    private val defaultSuggestedEmployees = listOf(
        "executive_assistant", "ai_receptionist", "sales", "operations",
        "customer_service", "technician", "finance", "admin",
        "marketing", "project_manager", "automation_specialist", "business_analyst"
    )

    val INDUSTRY_PRESETS: List<IndustryPreset> = listOf(
        IndustryPreset(
            id = "cleaning",
            name = "Cleaning & Maid Services",
            description = "Residential, commercial, deep cleaning, and move-out specialists.",
            defaultServices = listOf("Standard Clean", "Deep Clean", "Move-in / Move-out", "Office Sanitation", "Window & Baseboard Add-on"),
            recommendedTone = "Friendly, warm, dependable, and spotless",
            technicianRoleName = "Cleaning Specialist",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_deep_clean",
                serviceType = "Deep Clean",
                checklist = listOf(
                    "Kitchen: degrease stove backsplash, clean microwave & exterior cabinets",
                    "Bathrooms: scrub tile grout, sanitize fixtures, polish mirrors",
                    "Living & Bedrooms: dust ceiling fans, baseboards & vacuum under furniture",
                    "Floors: HEPA vacuum followed by microfiber disinfectant mop"
                ),
                preCheck = listOf("Confirm pet status and entry code", "Inspect fragile decor or pre-existing marks"),
                execute = listOf("Top-to-bottom dusting", "Kitchen deep scrub", "Bathroom sanitization", "Final floor mop"),
                cleanUp = listOf("Double-bag trash", "Pack soiled microfibers separately"),
                clientConfirmation = listOf("Send completion walkthrough notes & invite feedback"),
                requiredTools = listOf("HEPA vacuum", "Color-coded microfibers", "Grout brush", "Non-toxic disinfectant"),
                safetyNotes = listOf("Never mix bleach and ammonia", "Wear non-slip footwear on wet tile")
            )
        ),
        IndustryPreset(
            id = "pressure_washing",
            name = "Pressure Washing & Exterior",
            description = "Driveway wash, house soft wash, roof treatment, and deck restoration.",
            defaultServices = listOf("Driveway & Sidewalk Wash", "House Siding Soft Wash", "Roof Moss Treatment", "Deck Rejuvenation", "Commercial Storefront Wash"),
            recommendedTone = "Energetic, safety-conscious, and transformative",
            technicianRoleName = "Wash Technician",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_driveway_wash",
                serviceType = "Driveway & Sidewalk Wash",
                checklist = listOf(
                    "Pre-wet all surrounding grass, shrubs, and flower beds",
                    "Pre-treat oil stains with degreaser",
                    "Run rotary surface cleaner in overlapping grid passes",
                    "Flush slurry cleanly and post-treat concrete"
                ),
                preCheck = listOf("Inspect concrete cracks", "Cover exterior electrical outlets"),
                execute = listOf("Vegetation soak", "Surface cleaner passes", "High-volume rinse"),
                cleanUp = listOf("Re-rinse plants", "Uncover outlets and stow hoses"),
                clientConfirmation = listOf("Remind owner concrete remains slick while drying"),
                requiredTools = listOf("Commercial pressure washer", "Rotary surface cleaner", "Chemical injector"),
                safetyNotes = listOf("Wear eye shield & waterproof boots", "Never aim wand at fragile siding or people")
            )
        ),
        IndustryPreset(
            id = "landscaping",
            name = "Landscaping & Lawn Care",
            description = "Lawn maintenance, mulching, seasonal cleanup, and trimming.",
            defaultServices = listOf("Weekly Mow & Edge", "Spring / Fall Cleanup", "Mulch & Bed Weeding", "Hedge Trimming", "Aeration & Overseeding"),
            recommendedTone = "Polite, punctual, proud of craftsmanship",
            technicianRoleName = "Crew Foreman",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_mow_edge",
                serviceType = "Weekly Mow & Edge",
                checklist = listOf(
                    "Clear lawn of toys, twigs, and debris",
                    "Mow at seasonal deck height (3.5–4 in)",
                    "Trim along fences & beds, blade-edge driveway & curbs",
                    "Blow clippings cleanly off all paved walkways"
                ),
                preCheck = listOf("Check for sprinkler heads and pet gates"),
                execute = listOf("Striped mowing", "String trimming", "Vertical concrete edging"),
                cleanUp = listOf("Blow driveway & porch clean", "Latch rear gate securely"),
                clientConfirmation = listOf("Send completion confirmation that yard is pristine & gate latched"),
                requiredTools = listOf("Commercial mower", "String trimmer", "Stick edger", "Blower"),
                safetyNotes = listOf("Eye and ear protection mandatory", "Check slopes before riding equipment")
            )
        ),
        IndustryPreset(
            id = "handyman",
            name = "Handyman & Home Repair",
            description = "General repairs, drywall, fixture mounting, carpentry, and hardware.",
            defaultServices = listOf("Drywall Patch & Paint", "Door & Lock Replacement", "Fixture & Fan Mounting", "Plumbing Leak Fix", "Shelving & Carpentry"),
            recommendedTone = "Direct, honest, practical, and capable",
            technicianRoleName = "Lead Handyman",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_handyman",
                serviceType = "Drywall Patch & Paint",
                checklist = listOf(
                    "Scan studs and wiring before cutting drywall",
                    "Install backing lumber and fasten replacement sheetrock",
                    "Tape, mud 2-3 feathered coats, sand smooth, prime & blend paint"
                ),
                preCheck = listOf("Protect floors with drop cloth", "Verify paint color match"),
                execute = listOf("Clean cutout", "Backing & drywall patch", "Compound feathering & touch-up"),
                cleanUp = listOf("Shop-vac dust", "Wipe baseboards"),
                clientConfirmation = listOf("Walk homeowner through seamless wall repair"),
                requiredTools = listOf("Stud finder", "Drill", "Taping knives", "220-grit sanding block"),
                safetyNotes = listOf("Shut off breaker near outlets", "Wear dust mask while sanding")
            )
        ),
        IndustryPreset(
            id = "hvac",
            name = "HVAC & Climate Systems",
            description = "AC diagnostics, heating repair, seasonal tune-ups, and installations.",
            defaultServices = listOf("AC Diagnostic & Repair", "Furnace Tune-Up", "Heat Pump Install", "Duct Air Quality", "Smart Thermostat Setup"),
            recommendedTone = "Technical, reassuring, precise, and safety-rigorous",
            technicianRoleName = "HVAC Technician",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_ac_tuneup",
                serviceType = "AC Diagnostic & Repair",
                checklist = listOf(
                    "Lock out electrical disconnect and inspect filter",
                    "Test capacitor microfarad rating",
                    "Clean condenser coils and flush condensate drain",
                    "Measure supply/return delta-T (target 16–20°F)"
                ),
                preCheck = listOf("Check electrical disconnect", "Inspect indoor filter"),
                execute = listOf("Electrical diagnostics", "Coil wash", "Refrigerant & delta-T verification"),
                cleanUp = listOf("Secure all panel screws", "Verify disconnect latched"),
                clientConfirmation = listOf("Share digital amp-draw & temperature split readings"),
                requiredTools = listOf("Manifold gauges", "Clamp multimeter", "Fin comb"),
                safetyNotes = listOf("Discharge capacitors before contact", "Follow EPA Section 608 rules")
            )
        ),
        IndustryPreset(
            id = "plumbing",
            name = "Plumbing & Pipe Services",
            description = "Leak repair, water heaters, drain clearing, and fixture installs.",
            defaultServices = listOf("Emergency Water Leak Repair", "Water Heater Service", "Drain Clearing", "Sewer Inspection", "Valve & Fixture Install"),
            recommendedTone = "Responsive, reassuring, and clean-handed",
            technicianRoleName = "Journeyman Plumber",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_plumbing_leak",
                serviceType = "Emergency Water Leak Repair",
                checklist = listOf(
                    "Isolate main water shutoff valve",
                    "Trace pipe failure and lay containment drop cloths",
                    "Install PEX/copper coupling and pressure-test 15 minutes"
                ),
                preCheck = listOf("Confirm shutoff access", "Place water containment trays"),
                execute = listOf("Water isolation", "Pipe excision & coupling", "Static pressure check"),
                cleanUp = listOf("Wet-vac moisture", "Sanitize damp framing"),
                clientConfirmation = listOf("Demonstrate dry pressure hold & review warranty"),
                requiredTools = listOf("PEX crimp tool", "Pipe cutter", "Moisture meter"),
                safetyNotes = listOf("Depressurize pipes before cutting", "Use heat shield when soldering")
            )
        ),
        IndustryPreset(
            id = "electrical",
            name = "Electrical & Smart Power",
            description = "Panel upgrades, EV chargers, circuit diagnostics, and lighting.",
            defaultServices = listOf("Main Panel Upgrade", "EV Charger Install", "Circuit Diagnostic", "GFCI & Rewiring", "Smart Lighting"),
            recommendedTone = "Methodical, NEC code-compliant, and safety-first",
            technicianRoleName = "Master Electrician",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_electrical",
                serviceType = "EV Charger Install",
                checklist = listOf(
                    "Calculate panel load capacity",
                    "Verify lockout/tagout and zero voltage",
                    "Install breaker, conduit, torque lugs to spec & test GFCI"
                ),
                preCheck = listOf("Lockout/tagout main breaker", "Verify zero voltage with meter"),
                execute = listOf("Breaker mounting", "Conduit run", "Torque verification"),
                cleanUp = listOf("Label panel directory", "Vacuum wire trimmings"),
                clientConfirmation = listOf("Run live vehicle charge verification with homeowner"),
                requiredTools = listOf("Insulated tools", "Torque screwdriver", "Digital multimeter"),
                safetyNotes = listOf("Strict NEC compliance & lockout/tagout before touching bus bars")
            )
        ),
        IndustryPreset(
            id = "painting",
            name = "Painting & Staining",
            description = "Interior rooms, exterior siding, cabinet refinishing, and deck stain.",
            defaultServices = listOf("Interior Painting", "Exterior Coating", "Cabinet Refinishing", "Deck Staining", "Trim & Molding"),
            recommendedTone = "Artisanal, neat, and detail-focused",
            technicianRoleName = "Lead Painter",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_painting",
                serviceType = "Interior Painting",
                checklist = listOf(
                    "Drape furniture and mask trim with crisp painter tape",
                    "Spackle holes, sand smooth, apply two uniform coats",
                    "Peel tape cleanly and inspect under bright light"
                ),
                preCheck = listOf("Verify paint swatch & sheen", "Cover flooring with canvas"),
                execute = listOf("Surface prep", "Cut-in & first coat", "Second coat"),
                cleanUp = listOf("Reinstall switch plates", "Label touch-up can"),
                clientConfirmation = listOf("Walk room with homeowner under bright light"),
                requiredTools = listOf("Angled sash brush", "Roller kit", "Canvas drop cloths"),
                safetyNotes = listOf("Maintain ventilation", "Lock ladder stabilizers on stairs")
            )
        ),
        IndustryPreset(
            id = "roofing",
            name = "Roofing & Gutters",
            description = "Leak inspections, shingle repair, gutter cleaning, and storm assessments.",
            defaultServices = listOf("Roof Leak Inspection", "Gutter Cleaning & Guards", "Shingle Repair", "Storm Assessment", "Full Reroof Quote"),
            recommendedTone = "Vigilant, safety-first, and transparent",
            technicianRoleName = "Roof Specialist",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_roofing",
                serviceType = "Roof Leak Inspection",
                checklist = listOf(
                    "Inspect attic decking for stains",
                    "Examine chimney flashing, pipe boots, and valleys",
                    "Photograph vulnerability zones and sweep yard with magnet"
                ),
                preCheck = listOf("Tie off ladder 4:1", "Inspect harness anchor points"),
                execute = listOf("Perimeter & gutter check", "Flashing inspection", "Photo log"),
                cleanUp = listOf("Magnetic nail sweep around driveway & lawn"),
                clientConfirmation = listOf("Review inspection photos & repair options with homeowner"),
                requiredTools = listOf("Pitch gauge", "Fall arrest harness", "Magnetic sweeper"),
                safetyNotes = listOf("Use active fall arrest on steep or damp pitches")
            )
        ),
        IndustryPreset(
            id = "construction",
            name = "Construction & Remodeling",
            description = "Framing, concrete, commercial buildouts, and specialty contracting.",
            defaultServices = listOf("Kitchen / Bath Remodel", "Framing & Structural", "Concrete Pour & Finish", "Commercial Buildout", "Code Inspection"),
            recommendedTone = "Authoritative, code-compliant, and organized",
            technicianRoleName = "Site Foreman",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_construction",
                serviceType = "Site Walkthrough & Punch List",
                checklist = listOf(
                    "Verify posted permits & site safety gear",
                    "Inspect structural ties, rough-ins & GFCI boxes",
                    "Document daily site log & punch list"
                ),
                preCheck = listOf("Review stamped plans", "Morning safety huddle"),
                execute = listOf("Perimeter & rough-in check", "Punch-list capture"),
                cleanUp = listOf("Secure tools & log site progress"),
                clientConfirmation = listOf("Deliver updated timeline & punch list"),
                requiredTools = listOf("Laser measure", "GFCI tester", "PPE"),
                safetyNotes = listOf("Hard hats, boots, and eye protection active on site")
            )
        ),
        IndustryPreset(
            id = "auto_services",
            name = "Auto Services & Detailing",
            description = "Mobile mechanics, detailing, brakes, oil service, and fleet care.",
            defaultServices = listOf("Full Mobile Detail", "Mobile Oil & Filter", "Brake Pad & Rotor Service", "Battery Diagnostic", "Fleet Inspection"),
            recommendedTone = "Straightforward, trustworthy, and mechanical",
            technicianRoleName = "Master Technician",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_auto",
                serviceType = "Mobile Service & Inspection",
                checklist = listOf(
                    "Verify VIN, tire pressure, and fluid levels",
                    "Perform requested service & torque fasteners to spec",
                    "Wipe touchpoints clean before customer handoff"
                ),
                preCheck = listOf("Chock wheels", "Use rated jack stands if lifted"),
                execute = listOf("Diagnostic check", "Parts install / detail", "Spec torque check"),
                cleanUp = listOf("Wipe steering wheel & door handles", "Clean work mat"),
                clientConfirmation = listOf("Show worn vs new parts and review checklist"),
                requiredTools = listOf("Torque wrench", "OBD2 scanner", "Jack stands"),
                safetyNotes = listOf("Never work under vehicle supported solely by hydraulic jack")
            )
        ),
        IndustryPreset(
            id = "real_estate",
            name = "Real Estate & Property Mgmt",
            description = "Turnovers, walkthroughs, inspections, and owner coordination.",
            defaultServices = listOf("Tenant Move-Out Inspection", "Listing Prep Walkthrough", "Turnover Coordination", "Property Health Check", "Lease Follow-Up"),
            recommendedTone = "Responsive, polished, and proactive",
            technicianRoleName = "Property Inspector",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_real_estate",
                serviceType = "Tenant Move-Out Inspection",
                checklist = listOf(
                    "Test smoke/CO detectors & HVAC thermostat",
                    "Inspect plumbing traps & appliances",
                    "Collect keys/remotes and log photo condition report"
                ),
                preCheck = listOf("Confirm vacant access & utility status"),
                execute = listOf("Room-by-room photo survey", "Key & meter check"),
                cleanUp = listOf("Secure windows, deadbolts, and set eco thermostat"),
                clientConfirmation = listOf("Send itemized turnover report to property owner"),
                requiredTools = listOf("Laser meter", "GFCI outlet tester", "Flashlight"),
                safetyNotes = listOf("Verify unit vacancy before entering", "Wear shoe covers")
            )
        ),
        IndustryPreset(
            id = "general_small_business",
            name = "General Small Business",
            description = "Flexible service operations for any local trade, shop, or agency.",
            defaultServices = listOf("Standard Service Appointment", "Consultation & Estimate", "Emergency Service Call", "Maintenance Check", "Follow-Up Visit"),
            recommendedTone = "Clear, dependable, polite, and responsive",
            technicianRoleName = "Service Specialist",
            suggestedEmployees = defaultSuggestedEmployees,
            jobPack = JobPackTemplate(
                id = "jp_general",
                serviceType = "Standard Service Appointment",
                checklist = listOf(
                    "Review client request notes and service address",
                    "Confirm scope and pricing before starting work",
                    "Complete work, clean area, and confirm satisfaction"
                ),
                preCheck = listOf("Confirm appointment window", "Verify tools & parts"),
                execute = listOf("Arrival check-in", "Service delivery", "Quality review"),
                cleanUp = listOf("Remove debris and wipe work surfaces"),
                clientConfirmation = listOf("Collect sign-off and trigger digital invoice"),
                requiredTools = listOf("Standard service kit", "Mobile device"),
                safetyNotes = listOf("Keep walkways clear and follow site safety rules")
            )
        ),
        IndustryPreset(
            id = "professional_services",
            name = "Professional & Scientific Services",
            description = "Consulting, engineering, research, design, and technical advisory firms.",
            defaultServices = listOf("Technical Feasibility Study", "Operational Audit", "Compliance Review", "Implementation Roadmap", "Executive Advisory Session"),
            recommendedTone = "Formal, strategic, rigorous, and insight-driven",
            technicianRoleName = "Senior Technical Consultant",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_operational_audit",
                serviceType = "Operational Audit",
                checklist = listOf("Collect stakeholder input and standard operating procedures", "Map current workflow bottlenecks and error rates", "Conduct compliance alignment against ISO/industry benchmarks", "Model financial ROI and efficiency gains for proposed interventions", "Draft executive briefing deck with plain-language recommendations"),
                preCheck = listOf("Execute mutual NDA", "Access existing system telemetry and documentation"),
                execute = listOf("Stakeholder interviews", "Data flow mapping", "Gap analysis", "Synthesis and roadmap drafting"),
                cleanUp = listOf("Sanitize and archive client raw interview notes", "Review findings internally with Project Manager"),
                clientConfirmation = listOf("Deliver executive presentation to client leadership", "Schedule 30-day implementation review checkpoint"),
                requiredTools = listOf("Process mapping software", "Risk matrix framework", "Cost-benefit financial model"),
                safetyNotes = listOf("Adhere strictly to client data privacy and non-disclosure standards", "Enforce multi-factor authentication for data transfers")
            )
        ),
        IndustryPreset(
            id = "healthcare",
            name = "Healthcare, Medical & Dental",
            description = "Clinics, dental practices, physical therapy, home health, and wellness practices.",
            defaultServices = listOf("Patient Intake & Screening", "Hygiene & Sterilization Audit", "Appointment Reminders & Recalls", "Insurance Verification", "Patient Follow-up Care"),
            recommendedTone = "Empathetic, reassuring, strictly confidential, and clinical",
            technicianRoleName = "Clinical Coordinator",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_intake_screening",
                serviceType = "Patient Intake & Screening",
                checklist = listOf("Verify patient demographic and insurance eligibility", "Review updated medical history and drug allergy flags", "Verify signed HIPAA privacy acknowledgments", "Sterilize patient operatory room and unseal sterile instrument cassette"),
                preCheck = listOf("Confirm appointment 24 hours prior via SMS", "Review chart alerts"),
                execute = listOf("Patient greeting & vitals check", "Intake recording", "Treatment setup"),
                cleanUp = listOf("Autoclave sterilization wipe-down", "Sharps disposal in biohazard container"),
                clientConfirmation = listOf("Schedule next recall appointment", "Send post-care instructions"),
                requiredTools = listOf("Sterilization autoclave", "Digital charting system", "Blood pressure monitor"),
                safetyNotes = listOf("Strict universal precautions: gloves, mask, eye protection", "Protect patient PHI at all times")
            )
        ),
        IndustryPreset(
            id = "legal_accounting",
            name = "Legal & Accounting Practices",
            description = "Law firms, CPA practices, bookkeepers, and tax advisory services.",
            defaultServices = listOf("Client Intake & Conflict Check", "Tax Document Preparation", "Retainer Agreement Drafting", "Quarterly Financial Review", "Statutory Compliance Filing"),
            recommendedTone = "Rigorous, ethical, confidential, and detail-oriented",
            technicianRoleName = "Case / Account Specialist",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_client_intake_legal",
                serviceType = "Client Intake & Conflict Check",
                checklist = listOf("Run conflict of interest check against all parties involved", "Collect identity verification documents", "Draft engagement letter and scope of representation", "Set up trust/retainer accounting ledger", "Establish secure client portal access"),
                preCheck = listOf("Review prospective client inquiry", "Execute conflict screening search"),
                execute = listOf("Engagement drafting", "Retainer fee collection", "File matter onboarding"),
                cleanUp = listOf("Archive conflict search clearance report in matter file"),
                clientConfirmation = listOf("Deliver signed retainer counter-copy and onboarding packet"),
                requiredTools = listOf("Conflict check database", "Encrypted document portal", "Trust accounting ledger"),
                safetyNotes = listOf("Strict compliance with ethical attorney-client / accountant privilege", "Segregate trust funds from operating account immediately")
            )
        ),
        IndustryPreset(
            id = "restaurants",
            name = "Restaurants & Food Service",
            description = "Cafes, bistros, catering companies, ghost kitchens, and dining establishments.",
            defaultServices = listOf("Catering Package Proposal", "Health Inspection Pre-Audit", "Weekly Inventory & Supplier Order", "Staff Shift Dispatch", "VIP Guest Experience Follow-up"),
            recommendedTone = "Hospitality-driven, appetizing, fast-paced, and food-safety focused",
            technicianRoleName = "Kitchen / Ops Lead",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_health_preaudit",
                serviceType = "Health Inspection Pre-Audit",
                checklist = listOf("Verify walk-in and low-boy refrigeration temps are under 40°F", "Check hot holding wells maintain minimum 135°F", "Inspect dish machine sanitizer concentration with test strips (50-100 ppm chlorine or 200 ppm quat)", "Check labeling and dating on all prepped food containers", "Ensure handwash sinks have hot water, soap, paper towels, and clean handwashing signs"),
                preCheck = listOf("Calibrate food thermometers with ice bath", "Review local health code checklist"),
                execute = listOf("Line walkthrough", "Storage room check", "Sanitizer test", "Temperature log verification"),
                cleanUp = listOf("Dispose of any expired prepped goods immediately", "Mop dry storage floors"),
                clientConfirmation = listOf("Sign off daily HACCP compliance sheet"),
                requiredTools = listOf("Thermocouple probe thermometer", "Chemical sanitizer test strips", "HACCP log tablet"),
                safetyNotes = listOf("Never cross-contaminate raw poultry with ready-to-eat foods", "Maintain unobstructed access to kitchen handwash sinks")
            )
        ),
        IndustryPreset(
            id = "salons_spas",
            name = "Salons, Spas & Personal Care",
            description = "Hair salons, day spas, barbershops, nail studios, and aesthetic clinics.",
            defaultServices = listOf("VIP Service Booking", "Consultation & Skin/Hair Analysis", "Station Disinfection & Sanitization", "Package / Membership Sales", "Post-Service Aftercare Check-in"),
            recommendedTone = "Pampering, stylish, warm, and attentive",
            technicianRoleName = "Senior Stylist / Aesthetician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_station_sanitization",
                serviceType = "Station Disinfection & Sanitization",
                checklist = listOf("Submerge combs, shears, and guards in EPA-registered hospital grade disinfectant", "Wipe styling chair, shampoo bowl, and mirror with antibacterial solution", "Place clean, freshly laundered cape and towel at station", "Check client color formula or skin allergy history in client profile"),
                preCheck = listOf("Send 2-hour appointment reminder text with parking info"),
                execute = listOf("Client consultation", "Service execution", "Homecare product recommendation"),
                cleanUp = listOf("Sweep hair cuttings immediately into dustpan", "Sanitize station for next client"),
                clientConfirmation = listOf("Schedule 4-week rebooking before client leaves", "Send automated aftercare text next morning"),
                requiredTools = listOf("Barbicide jar & solution", "Autoclave/UV sterilizer", "Styling station tools"),
                safetyNotes = listOf("Perform skin patch test 24 hours prior for clients receiving new chemical coloring or tint", "Never use chipped or cracked glass tools")
            )
        ),
        IndustryPreset(
            id = "retail_ecommerce",
            name = "Retail & E-Commerce",
            description = "Boutiques, local specialty stores, online sellers, and omnichannel merchants.",
            defaultServices = listOf("Order Fulfillment & Shipping", "Product Return / Exchange", "Inventory Reorder & Restock", "Promotional Campaign Launch", "Customer Review Request"),
            recommendedTone = "Enthusiastic, helpful, prompt, and brand-forward",
            technicianRoleName = "Fulfillment Specialist",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_order_fulfillment",
                serviceType = "Order Fulfillment & Shipping",
                checklist = listOf("Pick items from warehouse/backroom matching order slip SKU and size", "Inspect merchandise for flaws, pulls, or manufacturing defects", "Wrap delicately with branded tissue and include thank-you note", "Weigh package on calibrated scale and print carrier shipping label", "Update tracking number in customer order management system"),
                preCheck = listOf("Verify shipping address passes postal carrier validation"),
                execute = listOf("Accurate item pick", "Quality inspection", "Protective pack & label application"),
                cleanUp = listOf("Stack outgoing parcels in carrier pickup bin", "Update inventory count"),
                clientConfirmation = listOf("Send customer tracking notification email with delivery estimate"),
                requiredTools = listOf("Thermal barcode label printer", "Digital parcel scale", "Handheld SKU scanner"),
                safetyNotes = listOf("Use box cutters with auto-retractable blades only", "Lift heavy shipping boxes using leg muscles, not back")
            )
        ),
        IndustryPreset(
            id = "nonprofits",
            name = "Nonprofits & Community Organizations",
            description = "Charities, community programs, foundations, and volunteer associations.",
            defaultServices = listOf("Donor Outreach & Stewardship", "Volunteer Shift Coordination", "Grant Proposal Tracking", "Community Event Logistics", "Impact Report Distribution"),
            recommendedTone = "Purpose-driven, grateful, inspiring, and transparent",
            technicianRoleName = "Program Coordinator",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_volunteer_coordination",
                serviceType = "Volunteer Shift Coordination",
                checklist = listOf("Confirm volunteer headcounts and shift roles 48 hours prior", "Prepare sign-in roster and safety liability waivers", "Set up orientation area with volunteer badges and task instructions", "Conduct 10-minute kickoff briefing and safety walkthrough", "Collect shift hours and issue appreciation certificates"),
                preCheck = listOf("Send venue directions, parking details, and dress code to volunteers"),
                execute = listOf("Check-in & waiver signing", "Mission kickoff briefing", "Shift supervision"),
                cleanUp = listOf("Return supplies to storage bins", "Tally total volunteer hours logged"),
                clientConfirmation = listOf("Send heartfelt thank-you email with group photo and impact metrics within 24 hours"),
                requiredTools = listOf("Tablet check-in app", "First aid kit", "Volunteer badges & lanyards"),
                safetyNotes = listOf("All volunteers must sign liability waivers and have emergency contacts on file", "Ensure water and shaded rest breaks are provided")
            )
        ),
        IndustryPreset(
            id = "it_services",
            name = "IT Services & Managed Technology (MSP)",
            description = "Helpdesk support, cloud migrations, network cybersecurity audits, workstation maintenance, and backup systems.",
            defaultServices = listOf("Managed IT Helpdesk (24/7)", "Cloud Migration (M365/Google)", "Cybersecurity Risk Audit", "Network & WiFi Deployment", "Disaster Recovery & Backup"),
            recommendedTone = "High-tech, dependable, cyber-vigilant, and jargon-free",
            technicianRoleName = "Senior Systems Engineer",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_cyber_audit",
                serviceType = "Cybersecurity Risk Audit",
                checklist = listOf("Scan company external IP ranges for open listening ports and vulnerabilities", "Audit multi-factor authentication (MFA) enforcement across all tenant accounts", "Verify immutable cloud backup retention policy and run test restore", "Inspect endpoint antivirus & EDR coverage across all corporate laptops", "Generate executive security risk score report and prioritized remediation roadmap"),
                preCheck = listOf("Secure written authorization from client executive leadership before running security scans"),
                execute = listOf("Vulnerability scan execution", "Active directory privilege review", "Backup recovery dry-run", "Threat model synthesis"),
                cleanUp = listOf("Purge temporary scan artifacts and log files securely"),
                clientConfirmation = listOf("Present visual compliance dashboard to C-suite", "Deliver 30-day remediation action plan"),
                requiredTools = listOf("Vulnerability scanner suite", "Cloud configuration compliance auditor", "Encrypted audit vault"),
                safetyNotes = listOf("Never disclose client credential weaknesses over unencrypted email channels", "Perform scans during non-peak hours to avoid production downtime")
            )
        ),
        IndustryPreset(
            id = "fitness_gyms",
            name = "Fitness Centers, Gyms & Personal Training",
            description = "Personal training studios, crossfit gyms, yoga/pilates studios, and athletic conditioning facilities.",
            defaultServices = listOf("1-on-1 Personal Training Session", "New Member Fitness Assessment", "Group Class Enrollment", "Nutrition & Lifestyle Coaching", "Gym Floor Safety & Equipment Maintenance"),
            recommendedTone = "Motivating, energizing, empowering, and health-first",
            technicianRoleName = "Head Coach / Fitness Specialist",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_member_assessment",
                serviceType = "New Member Fitness Assessment",
                checklist = listOf("Review health intake questionnaire (PAR-Q) and medical release", "Conduct resting vitals, body composition, and movement screening (FMS)", "Identify postural imbalances and joint mobility limitations", "Establish 90-day SMART fitness milestones (strength, endurance, body fat)", "Design custom phase-1 training regimen tailored to client baseline"),
                preCheck = listOf("Confirm client is hydrated and dressed in athletic footwear", "Sanitize screening mats and calipers"),
                execute = listOf("PAR-Q review", "Functional movement screen", "Strength baseline test", "Goal-setting consultation"),
                cleanUp = listOf("Upload biometric scores into client mobile tracking portal"),
                clientConfirmation = listOf("Book client first 3 personal training sessions", "Send warmup video guide via app"),
                requiredTools = listOf("Bioimpedance body composition scale", "Blood pressure monitor", "Movement screen dowel & mats"),
                safetyNotes = listOf("Stop assessment immediately if client experiences dizziness, nausea, or chest pressure", "Confirm AED defibrillator is inspected weekly")
            )
        ),
        IndustryPreset(
            id = "pet_care",
            name = "Pet Care, Veterinary & Grooming Services",
            description = "Mobile pet grooming, veterinary clinics, doggy daycare, pet boarding, and canine training.",
            defaultServices = listOf("Full-Service Pet Grooming & Spa", "Veterinary Wellness Exam", "Dog Daycare & Boarding Intake", "Canine Behavior Training", "Vaccine & Microchip Clinic"),
            recommendedTone = "Compassionate, gentle, pet-loving, and trustworthy",
            technicianRoleName = "Certified Pet Stylist / Vet Tech",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_pet_grooming",
                serviceType = "Full-Service Pet Grooming & Spa",
                checklist = listOf("Verify current rabies and core vaccination records with pet parent", "Conduct pre-groom snout-to-tail health inspection (ears, skin, mats, fleas)", "Hydro-massage bath with hypoallergenic organic oatmeal shampoo", "High-velocity warm air blow dry with eye and ear protective hoods", "Nail dremel grind smooth, sanitary trim, and breed-standard scissor cut"),
                preCheck = listOf("Confirm pet temperament and any known sensitivities", "Ensure grooming loop and table restraints are secure"),
                execute = listOf("Pre-bath brush out", "Bath & conditioning", "Force drying with ear protection", "Precision scissor styling"),
                cleanUp = listOf("Disinfect table surface, blades, and shears with Virkon/Barbicide", "Sweep hair into sealed bin"),
                clientConfirmation = listOf("Present groomed pet with festive bandana", "Report any discovered ear redness or skin tags to owner"),
                requiredTools = listOf("Hydraulic grooming table", "Variable speed blower", "Ceramic clipper blades", "Diamond nail dremel"),
                safetyNotes = listOf("Never leave any pet unattended on a grooming table or in a tub", "Use muzzles only when strictly required for animal/handler safety")
            )
        ),
        IndustryPreset(
            id = "event_production",
            name = "Event Planning, Catering & Media Production",
            description = "Weddings, corporate conferences, banquet catering, event photography/videography, and audio-visual staging.",
            defaultServices = listOf("Full-Service Event Planning", "Banquet Catering & Bar Service", "Event Photography & Cinema", "Audio/Visual Staging & Lighting", "Day-Of Event Coordination"),
            recommendedTone = "Sophisticated, celebratory, calm under pressure, and flawless",
            technicianRoleName = "Event Director / Lead Producer",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_event_runofshow",
                serviceType = "Day-Of Event Coordination",
                checklist = listOf("Distribute minute-by-minute run-of-show timeline to all vendors and stage crew", "Verify banquet seating charts, place cards, and dietary meal markers", "Conduct AV sound check, wireless mic frequency sweep, and video cue test", "Manage vendor load-in arrival schedule and dock parking access", "Coordinate stage transitions, speaker cueing, and live event milestones"),
                preCheck = listOf("Sync master clocks across production crew headsets", "Confirm VIP dressing room amenities"),
                execute = listOf("Vendor check-in", "Full tech rehearsal", "Guest doors open", "Run-of-show execution", "Send-off ceremony"),
                cleanUp = listOf("Oversee vendor strike and rental equipment return inventory", "Conduct venue walkthrough with facility manager"),
                clientConfirmation = listOf("Deliver gift box to client/couple", "Schedule post-event photo/video gallery delivery preview"),
                requiredTools = listOf("Production walkie-talkies & headsets", "Master timeline tablet", "Emergency event kit", "Digital audio recorder"),
                safetyNotes = listOf("Keep emergency exit doors completely clear of decor or cable trunks", "Tape down all floor cables with heavy gaffer tape")
            )
        ),
        IndustryPreset(
            id = "dental_practices",
            name = "Dental & Specialized Medical Clinics",
            description = "General family dentistry, orthodontics, cosmetic dental clinics, and specialized outpatient care.",
            defaultServices = listOf("Comprehensive Oral Exam & Cleaning", "Emergency Toothache Visit", "Cosmetic Whitening / Veneers", "Orthodontic / Clear Aligner Check", "Dental Implant Consultation"),
            recommendedTone = "Calming, clinically immaculate, gentle, and transparent",
            technicianRoleName = "Lead Dental Hygienist / Clinical Coordinator",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_dental_operatory",
                serviceType = "Comprehensive Oral Exam & Cleaning",
                checklist = listOf("Run ultrasonic cleaning and autoclave sterilization cycle on all dental handpieces", "Review patient medical history, allergies, and blood pressure baseline", "Take digital bitewing X-rays with lead protective thyroid collar", "Perform periodontal probing depth charting and ultrasonic calculus scaling", "Apply fluoride varnish and demonstrate personalized oral hygiene techniques"),
                preCheck = listOf("Place barrier film over operatory light handles, air/water syringe, and chair buttons"),
                execute = listOf("Patient greeting & health update", "Diagnostic digital imaging", "Periodontal cleaning", "Dentist clinical exam"),
                cleanUp = listOf("Dispose of single-use sharps in biohazard container", "Disinfect dental chair with hospital disinfectant wipes"),
                clientConfirmation = listOf("Schedule 6-month recall hygiene reservation", "Review insurance co-pay estimate at front desk"),
                requiredTools = listOf("Digital intraoral sensor & X-ray generator", "Piezo ultrasonic scaler", "Class-B vacuum autoclave", "Periodontal probe"),
                safetyNotes = listOf("Strict OSHA/CDC infection control compliance: fresh gloves, masks, and eye shields for every patient", "Sharps disposed immediately")
            )
        ),
        IndustryPreset(
            id = "moving_storage",
            name = "Moving, Storage & Logistics",
            description = "Residential home moving, commercial office relocation, crating, storage vaulting, and freight delivery.",
            defaultServices = listOf("Residential Home Move", "Commercial Office Relocation", "Full-Service Packing & Crating", "Short & Long Term Vault Storage", "Heavy Item & Piano Transport"),
            recommendedTone = "Careful, robust, punctual, and highly organized",
            technicianRoleName = "Moving Crew Lead / Dispatcher",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_residential_move",
                serviceType = "Residential Home Move",
                checklist = listOf("Lay down neoprene floor runners and padded door jamb protectors at pickup residence", "Pad wrap and shrink wrap all finished wood furniture, glass tops, and televisions", "Disassemble bed frames, dining table legs, and secure hardware in labeled bags", "Load moving van systematically: tier heavy boxes on bottom, strap uprights every 4 feet", "Deliver to destination, reassemble all furniture, and place items in designated rooms"),
                preCheck = listOf("Perform pre-move walkthrough with homeowner, noting pre-existing furniture scratches on digital bill of lading"),
                execute = listOf("Floor & wall protection setup", "Packing & wrapping", "Truck loading & logistics tie-down", "Transit", "Unload & staging"),
                cleanUp = listOf("Fold all moving pads and roll up floor runners", "Remove all used tape and shrink wrap debris from client home"),
                clientConfirmation = listOf("Walk through empty rooms with homeowner to confirm zero remaining items", "Collect digital signature on completed delivery receipt"),
                requiredTools = listOf("Moving blankets & stretch film", "4-wheel furniture dollies", "Appliance hand truck with straps", "Ratchet load bars"),
                safetyNotes = listOf("Bend knees and lift using core muscles; team-lift all items over 60 lbs", "Deploy truck ramp locks securely before rolling dollies")
            )
        ),
        IndustryPreset(
            id = "solar_cleanenergy",
            name = "Solar Energy & Battery Storage Systems",
            description = "Residential & commercial solar photovoltaic installation, Tesla Powerwall storage, EV integration, and grid tie-in.",
            defaultServices = listOf("Rooftop Solar PV Installation", "Home Battery Storage (Powerwall)", "Solar System Energy Audit", "Inverter Replacement & Troubleshooting", "Solar Panel Cleaning & Maintenance"),
            recommendedTone = "Innovative, eco-conscious, mathematically exact, and future-forward",
            technicianRoleName = "NABCEP Certified Solar Lead",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_solar_commissioning",
                serviceType = "Rooftop Solar PV Installation",
                checklist = listOf("Verify roof rafter structural attachments and lag bolt torque to 35 ft-lbs", "Flash all roof penetrations with code-approved metallic flashing plates", "Snap module rails straight and lay DC wiring in UV-resistant clips", "Install microinverters / DC optimizers and map serial numbers to layout grid", "Test DC open-circuit voltage (Voc) and verify polarity before landing at inverter disconnect", "Submit PTO (Permission to Operate) package to municipal electrical utility"),
                preCheck = listOf("Anchor fall protection safety ropes to certified roof ridge anchors", "Conduct morning tailboard electrical briefing"),
                execute = listOf("Stanchion & rail mounting", "Panel placement & clamping", "Conduit run & inverter wiring", "System commissioning test"),
                cleanUp = listOf("Sweep roof and grounds with magnetic nail sweeper", "Seal and paint exterior conduit to match house siding"),
                clientConfirmation = listOf("Configure homeowner solar monitoring app on their smartphone", "Review emergency system shutoff switch location"),
                requiredTools = listOf("NABCEP digital solar multimeter", "Torque wrench with calibration cert", "Fall arrest harness & ropes", "MC4 crimp tool"),
                safetyNotes = listOf("Solar panels generate lethal DC voltage immediately upon exposure to sunlight; never touch bare conductor ends", "Tie off 100% on roofs")
            )
        ),
        IndustryPreset(
            id = "marketing_creative",
            name = "Marketing Agencies & Creative Studios",
            description = "Digital marketing, paid social ad campaigns, SEO, website design, brand development, and video production.",
            defaultServices = listOf("Monthly Retainer Marketing Growth", "Paid Meta & Google Ads Management", "SEO & Organic Content Strategy", "Custom Website Design & Launch", "Brand Identity & Design System"),
            recommendedTone = "Creative, strategic, metric-driven, and high-energy",
            technicianRoleName = "Growth Strategist / Creative Director",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_ad_campaign_launch",
                serviceType = "Paid Meta & Google Ads Management",
                checklist = listOf("Verify conversion tracking pixels, Google Tag Manager, and server-side CAPI events", "Develop 3 high-converting creative hooks, ad copy variations, and video reels", "Configure campaign target audiences, exclusions, and custom lookalikes", "Set up automated bidding rules to protect target ROAS and cost-per-lead limits", "Launch campaign and conduct day-1 budget pacing & impression share check"),
                preCheck = listOf("Review client brand guidelines and verify advertising account billing credit line"),
                execute = listOf("Tracking pixel verification", "Ad set creation", "A/B creative testing upload", "Campaign live activation"),
                cleanUp = listOf("Archive draft ads and document UTM tracking parameter naming syntax in client master file"),
                clientConfirmation = listOf("Send live campaign preview links to client stakeholder with expected 7-day learning phase benchmarks"),
                requiredTools = listOf("Meta Ads Manager", "Google Ads Editor", "Conversion API test console", "Creative design suite"),
                safetyNotes = listOf("Double-check daily budget decimal points before publishing; avoid accidental overspend spikes", "Strict adherence to FTC endorsement guidelines")
            )
        ),
        IndustryPreset(
            id = "hospitality_rentals",
            name = "Hospitality & Short-Term Rental Management",
            description = "Airbnb/VRBO property management, boutique hotels, guest turnover operations, and concierge services.",
            defaultServices = listOf("Same-Day Guest Turnover & Staging", "24/7 Guest Concierge & Communication", "Property Maintenance & Emergency Fix", "Smart Lock & Keyless Access Dispatch", "Listing Optimization & Pricing Algorithm"),
            recommendedTone = "Luxurious, welcoming, hyper-responsive, and 5-star hospitality",
            technicianRoleName = "Hospitality Operations Lead",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_turnover_staging",
                serviceType = "Same-Day Guest Turnover & Staging",
                checklist = listOf("Inspect property for any guest damage or left-behind personal belongings", "Strip and launder all bed linens and bath towels at high sanitizing temperature", "Deep clean kitchen, run dishwasher, and empty ice maker tray", "Restock luxury amenities (artisan coffee, shampoo, conditioner, wine welcome gift)", "Reset smart thermostat to welcoming 70°F and update smart lock access PIN for incoming guest", "Upload 15-point staging proof photo log to owner dashboard"),
                preCheck = listOf("Confirm previous guest has checked out via smart lock log before entering"),
                execute = listOf("Damage inspection walkthrough", "Linen swap & sanitization", "Deep clean & bathroom reset", "Welcome staging", "Smart code reset"),
                cleanUp = listOf("Double bag and dispose of all trash in exterior curbside bins", "Lock all perimeter windows"),
                clientConfirmation = listOf("Send incoming guest personalized check-in guide with door code and local restaurant recommendations"),
                requiredTools = listOf("Hotel-grade linen pack", "Smart lock admin app", "Inspection checklist camera tablet"),
                safetyNotes = listOf("Check smoke detectors and carbon monoxide sensors during every turnover", "Ensure first aid kit and fire extinguisher are inspected")
            )
        ),
        IndustryPreset(
            id = "education_tutoring",
            name = "Education, Tutoring & Training Academies",
            description = "Private tutoring centers, test prep academies, music & arts schools, STEM learning programs, and language institutes.",
            defaultServices = listOf("Academic Tutoring (1-on-1)", "Standardized Test Prep (SAT/ACT)", "Student Learning Style Assessment", "Curriculum Progress Tracking", "Parent Milestone Review Conference"),
            recommendedTone = "Encouraging, intellectual, structured, and student-focused",
            technicianRoleName = "Lead Academic Mentor / Instructor",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_student_diagnostic",
                serviceType = "Student Learning Style Assessment",
                checklist = listOf("Administer customized diagnostic skills evaluation in core subject areas", "Identify conceptual learning gaps and strengths through oral reasoning prompts", "Formulate an individualized 12-week learning masterplan with measurable goals", "Assign tailored learning modules and interactive practice resources", "Schedule recurring weekly tutoring timeslot aligned with student study rhythm"),
                preCheck = listOf("Review student past report cards, teacher notes, and learning style preferences"),
                execute = listOf("Diagnostic testing session", "Student motivation discussion", "Curriculum mapping", "Parent consultation"),
                cleanUp = listOf("Record diagnostic score breakdowns in student digital academic portfolio"),
                clientConfirmation = listOf("Provide parents with comprehensive written assessment report and 12-week roadmap"),
                requiredTools = listOf("Diagnostic testing suite", "Interactive whiteboarding tablet", "Student progress tracking portal"),
                safetyNotes = listOf("Maintain background check clearances and strict child safety and FERPA privacy compliance across all staff", "Glass view windows on all tutoring rooms")
            )
        ),
        IndustryPreset(
            id = "property_operations",
            name = "Commercial & Residential Property Management",
            description = "Multi-family apartments, commercial office buildings, HOA communities, and industrial facilities operations.",
            defaultServices = listOf("Tenant Move-In / Move-Out Audit", "24/7 Facility Maintenance Dispatch", "Lease Renewal & Rent Collection", "Vendor Contract & Bid Management", "Annual Life-Safety & Code Audit"),
            recommendedTone = "Authoritative, proactive, compliance-strict, and community-minded",
            technicianRoleName = "Facility Operations Manager",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_facility_audit",
                serviceType = "Annual Life-Safety & Code Audit",
                checklist = listOf("Inspect commercial fire alarm pull stations and verify annual certified tag", "Test emergency egress lighting battery backups for 90-second illumination", "Inspect elevator maintenance log and valid municipal certificate of operation", "Verify roof drainage scuppers and stormwater retention drains are clear of debris", "Audit ADA handicap parking stalls and ramp grade signage compliance"),
                preCheck = listOf("Notify all building tenants 48 hours in advance regarding life-safety test schedule"),
                execute = listOf("Common area walkthrough", "Mechanical & electrical room audit", "Fire equipment test", "Log reconciliation"),
                cleanUp = listOf("Stow testing gear and return all elevator controls to normal automated service"),
                clientConfirmation = listOf("Submit signed compliance certification to property ownership board and insurer"),
                requiredTools = listOf("Emergency light battery load tester", "Master facility key set", "Code compliance tablet"),
                safetyNotes = listOf("Never bypass or mute fire alarm panels during testing without prior notification to local fire department dispatch", "Keep electrical rooms locked")
            )
        ),
        IndustryPreset(
            id = "security_locksmith",
            name = "Security Systems, Locksmith & Access Control",
            description = "Emergency locksmith, commercial access control cards, smart locks, CCTV surveillance cameras, and alarm systems.",
            defaultServices = listOf("Emergency Lockout & Rekeying", "Commercial Card Access Control", "HD Security Camera CCTV Install", "Smart Lock & Deadbolt Retrofit", "Intrusion Alarm System Setup"),
            recommendedTone = "Discreet, highly secure, rapid response, and certified",
            technicianRoleName = "Licensed Master Locksmith / Security Tech",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_access_control_install",
                serviceType = "Commercial Card Access Control",
                checklist = listOf("Cut clean mortise pocket in commercial metal door jamb for electric strike", "Pull plenum-rated 18/4 shielded security wire through door frame conduit", "Mount RFID / biometric badge reader at standard 42-inch ADA height", "Wire fail-secure power supply with backup gel-cell batteries", "Configure access permissions, door schedules, and audit log tracking in cloud software", "Test crash bar mechanical egress to verify immediate unimpeded emergency exit"),
                preCheck = listOf("Verify client legal authority and building ownership before altering access hardware"),
                execute = listOf("Jamb preparation & electric strike landing", "Low-voltage wiring", "Controller termination", "Software credential testing"),
                cleanUp = listOf("Vacuum metal filing debris from entryway", "Calibrate door closer speed so door latches securely"),
                clientConfirmation = listOf("Enroll building administrator master badges and demonstrate emergency lockdown trigger"),
                requiredTools = listOf("Mortise jig & carbide router", "Digital multimeter", "Wire fish rods", "Security bit driver set"),
                safetyNotes = listOf("Fire code compliance is paramount: doors must allow free mechanical egress from interior during power outages without keys", "Wear eye protection while cutting steel frames")
            )
        ),
        IndustryPreset(
            id = "pest_control",
            name = "Pest Control & Extermination",
            description = "General pest treatments, termite inspection and treatment, rodent exclusion, mosquito and tick yard treatments, and bed bug remediation.",
            defaultServices = listOf("General Pest Treatment", "Termite Inspection & Treatment", "Rodent Exclusion", "Mosquito & Tick Yard Treatment", "Bed Bug Heat Treatment"),
            recommendedTone = "Reassuring, thorough, safety-first, and fast to respond",
            technicianRoleName = "Licensed Pest Control Technician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_termite_treatment",
                serviceType = "Termite Inspection & Treatment",
                checklist = listOf("Walk the full exterior and probe sill plates, door frames, and foundation joints for mud tubes and hollow wood", "Inspect crawl space and basement for moisture intrusion and prior treatment drill marks", "Map active infestation zones and photograph evidence for the customer report", "Apply labeled termiticide barrier treatment per product label and state regulations", "Install or inspect in-ground monitoring bait stations at labeled intervals", "Leave the customer a written treatment summary, re-treatment warranty terms, and prevention checklist"),
                preCheck = listOf("Confirm pets and children are clear of treatment zones", "Verify product label, dilution rate, and state license requirements"),
                execute = listOf("Exterior inspection and evidence mapping", "Barrier treatment application", "Bait station installation or service"),
                cleanUp = listOf("Remove drill dust and debris", "Secure all chemical containers for transport"),
                clientConfirmation = listOf("Walk the customer through findings, treatment zones, and the warranty"),
                requiredTools = listOf("Termite probe and moisture meter", "Power sprayer and calibrated nozzles", "Drill with masonry bits", "Bait station auger", "PPE: respirator, gloves, eye protection"),
                safetyNotes = listOf("Never apply termiticide near wells, drains, or waterways except exactly as the product label permits", "Keep children and pets away from treated soil until dry")
            )
        ),
        IndustryPreset(
            id = "pool_spa_services",
            name = "Pool & Spa Services",
            description = "Pool cleaning and chemical balancing, pump and heater repair, salt system conversion, and seasonal pool opening and closing.",
            defaultServices = listOf("Pool Cleaning & Chemical Balancing", "Pump & Equipment Repair", "Heater Installation & Repair", "Pool Opening & Closing", "Salt System Conversion"),
            recommendedTone = "Friendly, reliable, detail-obsessed, and crystal clear",
            technicianRoleName = "Certified Pool Technician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_pool_equipment_repair",
                serviceType = "Pump & Equipment Repair",
                checklist = listOf("Test water chemistry and record chlorine, pH, alkalinity, and calcium hardness before touching equipment", "Inspect pump basket, impeller, seals, and motor for cracks, debris, and bearing noise", "Check filter pressure gauge against clean baseline and backwash or clean cartridges as needed", "Test heater ignition, thermostat, and safety shutoffs; inspect for gas or water leaks", "Prime the pump, verify full circulation, and re-check for leaks at all unions", "Log all readings and parts replaced for the customer service record"),
                preCheck = listOf("Shut off power at the breaker before opening any pump or heater housing", "Confirm gate access and pet status"),
                execute = listOf("Water chemistry test", "Pump and filter diagnosis", "Part replacement and reassembly", "Heater safety test"),
                cleanUp = listOf("Remove old seals, baskets, and packaging from the equipment pad"),
                clientConfirmation = listOf("Show the customer the running system, pressure reading, and chemistry results"),
                requiredTools = listOf("Water test kit", "Pump seal and impeller tools", "Multimeter", "PVC primer, cement, and unions", "Filter cleaning wand"),
                safetyNotes = listOf("Never run a pump dry — it destroys the seal in minutes", "Treat all pool electrical work as live until the breaker is locked out and verified")
            )
        ),
        IndustryPreset(
            id = "appliance_repair",
            name = "Appliance Repair",
            description = "Diagnosis and repair of washers, dryers, refrigerators, ovens, ranges, and dishwashers, including parts replacement.",
            defaultServices = listOf("Washer & Dryer Repair", "Refrigerator & Freezer Repair", "Oven & Range Repair", "Dishwasher Repair", "Diagnostic & Parts Replacement"),
            recommendedTone = "Straightforward, honest about parts and cost, and respectful of the home",
            technicianRoleName = "Appliance Repair Technician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_washer_repair",
                serviceType = "Washer & Dryer Repair",
                checklist = listOf("Ask the customer for the exact symptom, error code, and when it started before opening the machine", "Verify power, water supply valves, and drain hose placement before diagnosing parts", "Run the machine through a short cycle and record noises, leaks, and error codes", "Test the suspect part (pump, belt, valve, control board) with a multimeter before replacing it", "Replace only the failed part, reassemble, and run a full test cycle with no leaks", "Explain the repair, the part warranty, and what to watch for in plain language"),
                preCheck = listOf("Unplug the machine and shut off water supply before removing panels", "Lay down a mat to protect the floor"),
                execute = listOf("Symptom interview and test cycle", "Part testing and replacement", "Full verification cycle"),
                cleanUp = listOf("Wipe down the machine exterior and remove old parts and packaging"),
                clientConfirmation = listOf("Run the machine with the customer present and confirm the symptom is gone"),
                requiredTools = listOf("Multimeter", "Nut driver and socket set", "Pliers and hose clamp tool", "Appliance dolly and moving straps"),
                safetyNotes = listOf("Never bypass a lid switch or thermal fuse to test a machine — both are safety devices", "Drain standing water before tipping a washer to avoid electrical and floor damage")
            )
        ),
        IndustryPreset(
            id = "garage_door",
            name = "Garage Door Services",
            description = "Garage door installation, broken spring replacement, opener installation and repair, track and cable repair, and smart garage controls.",
            defaultServices = listOf("Garage Door Installation", "Broken Spring Replacement", "Opener Installation & Repair", "Track & Cable Repair", "Smart Garage Controls"),
            recommendedTone = "Safety-obsessed, prompt, and plain-spoken about cost",
            technicianRoleName = "Garage Door Technician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_spring_replacement",
                serviceType = "Broken Spring Replacement",
                checklist = listOf("Confirm the door is fully closed and secured before touching any spring hardware", "Identify spring type (torsion vs extension), wire size, length, and wind direction", "Release torsion tension with winding bars only — never with screwdrivers or pliers", "Replace both springs as a matched pair when one breaks, and inspect cables, drums, and bearings", "Re-tension to the correct number of turns, lubricate moving parts, and test balance by hand", "Test safety reverse sensors and auto-reverse, then program remotes and keypads"),
                preCheck = listOf("Secure the door and disconnect the opener", "Verify spring measurements against the door weight and height"),
                execute = listOf("Tension release", "Spring and hardware replacement", "Re-tension and balance test"),
                cleanUp = listOf("Remove broken spring fragments and packaging from the garage floor"),
                clientConfirmation = listOf("Operate the door with the customer and demonstrate the safety reverse test"),
                requiredTools = listOf("Winding bars (matched pair)", "Vise grips and locking pliers", "Socket and wrench set", "Cable and drum tools", "Lubricant"),
                safetyNotes = listOf("Torsion springs store lethal energy: winding bars must be fully seated before every quarter turn, and your body must never be in the spring line of travel", "Never work on a partially open door held only by the opener")
            )
        ),
        IndustryPreset(
            id = "flooring",
            name = "Flooring Installation & Refinishing",
            description = "Hardwood installation and refinishing, luxury vinyl plank, tile installation, carpet installation, and floor repair and sanding.",
            defaultServices = listOf("Hardwood Installation & Refinishing", "Luxury Vinyl Plank", "Tile Installation", "Carpet Installation", "Floor Repair & Sanding"),
            recommendedTone = "Craft-focused, careful with the home, and clear about dust and dry times",
            technicianRoleName = "Flooring Installer",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_hardwood_refinish",
                serviceType = "Hardwood Installation & Refinishing",
                checklist = listOf("Measure moisture content of the wood and subfloor and confirm it is within range before sanding", "Inspect for loose boards, squeaks, pet stains, and prior wax coatings that block adhesion", "Sand in grit progression (coarse to fine), edging and corners included, with dust containment running", "Vacuum and tack the floor twice between sanding and the first finish coat", "Apply stain (if chosen) then finish coats, keeping coats thin and even with proper dry time between them", "Walk the floor in raking light with the customer and note the cure time before furniture returns"),
                preCheck = listOf("Protect adjacent rooms with plastic sheeting and confirm ventilation", "Remove furniture, rugs, and wall hangings from the work area"),
                execute = listOf("Moisture check and repair", "Sanding progression", "Stain and finish coats"),
                cleanUp = listOf("Vacuum all dust from vents, sills, and baseboards before final coat"),
                clientConfirmation = listOf("Final walkthrough in raking light and written cure-time instructions"),
                requiredTools = listOf("Drum sander and edger", "Moisture meter", "Vacuum with fine dust filter", "Applicator pads and finish", "Respirator and hearing protection"),
                safetyNotes = listOf("Oil-soaked rags can spontaneously combust — lay them flat outdoors or store in a sealed metal container", "Wear a respirator during all sanding passes")
            )
        ),
        IndustryPreset(
            id = "fence_deck",
            name = "Fence & Deck Building",
            description = "Wood, vinyl, and chain-link fence installation, deck building and resurfacing, gate and opener installation, and fence repair and staining.",
            defaultServices = listOf("Wood Fence Installation", "Vinyl & Chain-Link Fencing", "Deck Building & Resurfacing", "Gate & Opener Installation", "Fence Repair & Staining"),
            recommendedTone = "Outdoorsy, dependable, and straight about materials and timelines",
            technicianRoleName = "Fence & Deck Builder",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_fence_install",
                serviceType = "Wood Fence Installation",
                checklist = listOf("Confirm property lines, easements, and HOA or permit rules before digging a single hole", "Call 811 and verify all underground utilities are marked along the fence line", "Set corner and end posts first, braced and plumb, then run a string line for the full run", "Set line posts at consistent spacing with concrete footings below frost depth where required", "Hang rails level, attach pickets with consistent gaps, and cap or stain as specified", "Walk the full line with the customer, check every gate latch, and haul away all spoils"),
                preCheck = listOf("Utility locate (811) and property line confirmation", "Material delivery and staging"),
                execute = listOf("Post layout and setting", "Rail and picket installation", "Gate hanging and hardware"),
                cleanUp = listOf("Grade and seed disturbed soil", "Remove concrete bags and cutoffs from the yard"),
                clientConfirmation = listOf("Full-line walkthrough and gate operation demo with the customer"),
                requiredTools = listOf("Post hole digger and auger", "Level and string line", "Circular saw and nail gun", "Concrete mixer", "Gate hardware kit"),
                safetyNotes = listOf("Never dig before utilities are marked — a gas or electric strike is the top risk on this job", "Brace posts before backfilling so a shifting post cannot fall on a worker")
            )
        ),
        IndustryPreset(
            id = "septic_services",
            name = "Septic Services",
            description = "Septic tank pumping, septic inspection and certification, drain field repair, septic system installation, and grease trap pumping.",
            defaultServices = listOf("Septic Tank Pumping", "Septic Inspection & Certification", "Drain Field Repair", "Septic System Installation", "Grease Trap Pumping"),
            recommendedTone = "No-nonsense, discreet, and clear about what the system needs and when",
            technicianRoleName = "Licensed Septic Technician",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_septic_pumping",
                serviceType = "Septic Tank Pumping",
                checklist = listOf("Locate the tank using the as-built drawing, probe, or locator — never guess and dig blind", "Expose lids, inspect risers and baffles, and measure scum and sludge layers before pumping", "Pump both compartments fully, backflush as needed, and inspect the tank walls for cracks or root intrusion", "Check the outlet filter, clean or replace it, and verify flow to the drain field", "Record tank size, gallons pumped, and condition notes for the customer and the county record", "Restore the lawn, close lids securely, and explain the pumping interval in plain terms"),
                preCheck = listOf("Confirm tank location and access, and keep children and pets clear of open lids", "Verify disposal site and manifest requirements"),
                execute = listOf("Lid exposure and inspection", "Pumping and backflush", "Filter service and flow check"),
                cleanUp = listOf("Reseed and grade disturbed turf", "Disinfect tools and wash down the work area"),
                clientConfirmation = listOf("Show the customer the empty tank and filter, and hand over the service report"),
                requiredTools = listOf("Vacuum pump truck and hoses", "Tank probe and lid lifting tools", "Sludge judge sampler", "Replacement outlet filters", "PPE: gloves, boots, eye protection"),
                safetyNotes = listOf("Septic gases are toxic and oxygen-displacing — never enter a tank, and keep your head out of an open riser", "Open lids are a fall hazard: guard them until they are closed again")
            )
        ),
        IndustryPreset(
            id = "tree_service",
            name = "Tree Service & Arbor Care",
            description = "Tree removal, trimming and pruning, stump grinding, storm damage cleanup, and certified arborist health assessments.",
            defaultServices = listOf("Tree Removal", "Tree Trimming & Pruning", "Stump Grinding", "Storm Damage Cleanup", "Arborist Health Assessments"),
            recommendedTone = "Safety-first, confident, and respectful of the property and neighbors",
            technicianRoleName = "Certified Arborist / Tree Climber",
            suggestedEmployees = listOf("executive_assistant", "sales", "operations", "customer_service", "technician", "finance", "admin", "marketing", "project_manager", "automation_specialist", "hr", "business_analyst"),
            jobPack = JobPackTemplate(
                id = "jp_tree_removal",
                serviceType = "Tree Removal",
                checklist = listOf("Assess lean, deadwood, decay, and targets (house, lines, fences) and choose the removal method before cutting", "Confirm power line clearance and call the utility for a drop if any limb is within reach of lines", "Set the drop zone, rope rigging plan, and ground crew positions, and brief every worker on escape routes", "Remove limbs in controlled, rigged sections — never free-fall large wood over a roof or fence", "Fell or crane-lift the trunk in sections, then grind the stump and clean all debris", "Walk the property with the customer and confirm lawn, beds, and structures are undamaged"),
                preCheck = listOf("Site hazard assessment and utility clearance", "Rigging plan and crew safety briefing"),
                execute = listOf("Rigged limb removal", "Trunk sectioning", "Stump grinding"),
                cleanUp = listOf("Chip brush, rake the lawn, and blow off drives and roofs"),
                clientConfirmation = listOf("Final property walkthrough with the customer"),
                requiredTools = listOf("Climbing saddle, ropes, and rigging gear", "Chainsaws (top-handle and ground)", "Stump grinder", "Chipper and truck", "PPE: helmet, eye and hearing protection, chaps"),
                safetyNotes = listOf("Tree work is high-risk: no one works under a suspended load, and no cutting happens without a clear escape route", "Keep all workers and bystanders at least two tree lengths from felling operations where possible")
            )
        ),
    )

    fun findPreset(industryId: String): IndustryPreset =
        INDUSTRY_PRESETS.firstOrNull { it.id.equals(industryId, ignoreCase = true) }
            ?: INDUSTRY_PRESETS.last()
}
