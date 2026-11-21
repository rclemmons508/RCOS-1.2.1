package com.rcsolutions.rcosmobile.data

import com.rcsolutions.rcosmobile.model.AIEmployee
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
        )
    )

    fun findPreset(industryId: String): IndustryPreset =
        INDUSTRY_PRESETS.firstOrNull { it.id.equals(industryId, ignoreCase = true) }
            ?: INDUSTRY_PRESETS.last()
}
