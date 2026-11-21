package com.example.detection

import com.example.model.AppLanguage
import java.util.Locale

/**
 * Detection v2 + Language Priority Cascade.
 *
 * Key guarantees:
 * 1. Separate rule sets for Skip Detection and Ad Detection.
 * 2. Language Priority Cascade with EARLY EXIT in every scan:
 *    - Step 0: View ID / structural signals (language-neutral)
 *    - Step 1: Selected app language (e.g. Bangla when Bangla is selected)
 *    - Step 2: English (or Hindi if English was selected)
 *    - Step 3: Remaining 3rd language safety net
 * 3. Strong vs Weak Ad Signals:
 *    - Any 1 STRONG signal starts AD_ACTIVE.
 *    - WEAK signals (Ad badge, Sponsored, advertiser CTA — Download/Install excluded)
 *      require >= 2 independent weak categories to start AD_ACTIVE.
 *    - Sponsored ALONE produces OVERLAY_SPONSORED (never mutes or announces).
 * 4. Strict Safety Blacklist prevents ever clicking Subscribe, Like, Dislike, Next,
 *    Play, Pause, Share, or countdown text ("Skip in 5").
 */
object DetectionEngine {

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    // Step 0: Language-neutral Skip View IDs (checked first)
    private val SKIP_VIEW_ID_SUBSTRINGS = listOf(
        "skip_ad_button",
        "modern_skip_ad_button",
        "skip_ad_button_text",
        "skip_ad",
        "skip_button"
    )

    // Step 0: Language-neutral Structural Ad View IDs
    private val STRONG_AD_VIEW_ID_SUBSTRINGS = listOf(
        "ad_progress_text",
        "ad_countdown",
        "ad_pod_label",
        "ad_duration",
        "skip_ad_button_container"
    )

    private val WEAK_AD_VIEW_ID_SUBSTRINGS = listOf(
        "ad_badge",
        "ad_info_button",
        "player_learn_more_button",
        "ad_cta_button"
    )

    // Safety Blacklist: NEVER click any node matching these labels
    private val BLACKLIST_EXACT_OR_TOKEN = setOf(
        "subscribe", "subscribed", "unsubscribe",
        "like", "dislike", "i like this", "i dislike this",
        "next", "next video", "previous", "previous video",
        "play", "pause", "play video", "pause video", "replay",
        "share", "download", "install", "save", "remix", "clip",
        "visit advertiser", "learn more", "shop now", "get offer", "sign up",
        "skip 10 seconds", "skip forward", "skip back", "skip backward",
        "সাবস্ক্রাইব", "সাবস্ক্রাইব করুন", "লাইক", "ডিসলাইক", "পরবর্তী", "চালান", "বিরতি", "শেয়ার", "ডাউনলোড", "ইনস্টল",
        "सदस्यता लें", "सब्सक्राइब", "लाइक", "डिसलाइक", "अगला", "चलाएं", "रोकें", "शेयर", "डाउनलोड", "इंस्टॉल"
    )

    // Per-language Skip button exact aliases (Layer 2)
    private val SKIP_TEXT_ALIASES: Map<AppLanguage, Set<String>> = mapOf(
        AppLanguage.BANGLA to setOf(
            "বিজ্ঞাপন এড়িয়ে যান",
            "বিজ্ঞাপন এড়িয়ে যান",
            "বিজ্ঞাপন এড়িয়ে যাও",
            "বিজ্ঞাপন এড়িয়ে যাও",
            "বিজ্ঞাপন এড়িয়ে যেতে পারেন",
            "বিজ্ঞাপন এড়িয়ে যেতে পারেন",
            "বিজ্ঞাপন স্কিপ করুন",
            "স্কিপ অ্যাড",
            "অ্যাড স্কিপ করুন"
        ),
        AppLanguage.ENGLISH to setOf(
            "skip ad",
            "skip ads",
            "skip"
        ),
        AppLanguage.HINDI to setOf(
            "विज्ञापन छोड़ें",
            "विज्ञापन छोड़ें",
            "विज्ञापन स्किप करें",
            "विज्ञापन छोड़ सकते हैं",
            "विज्ञापन छोड़ सकते हैं",
            "स्किप ऐड"
        )
    )

    // Countdown regex patterns per language (Ad detected = STRONG, NO click!)
    private val COUNTDOWN_REGEXES: Map<AppLanguage, List<Regex>> = mapOf(
        AppLanguage.ENGLISH to listOf(
            Regex("""(?i)\bskip\s*(ad|ads)?\s*in\s*\d+"""),
            Regex("""(?i)you\s+can\s+skip\b.*\d+"""),
            Regex("""(?i)\bskip\s+in\s+\d+\s*(s|sec|secs|second|seconds)?\b"""),
            Regex("""(?i)^video\s+will\s+play\s+after\s+ad\b.*"""),
            Regex("""(?i)^your\s+video\s+will\s+begin\s+in\b.*"""),
            Regex("""(?i)\bad\s+will\s+end\b.*"""),
            Regex("""(?i)\bad\s+ends\s+in\b.*""")
        ),
        AppLanguage.BANGLA to listOf(
            Regex("""(বিজ্ঞাপন|অ্যাড).*[০-৯0-9]+.*(সেকেন্ড|পর|পরে|মধ্যে)"""),
            Regex("""[০-৯0-9]+\s*(সেকেন্ড|সে\.)?\s*(পর|পরে|মধ্যে).*(বিজ্ঞাপন|এড়িয়ে|এড়িয়ে|স্কিপ)"""),
            Regex("""(বিজ্ঞাপন|অ্যাড).*(পরে|শেষ\s*হলে)\s*ভিডিও"""),
            Regex("""বিজ্ঞাপন.*শেষ\s*হবে""")
        ),
        AppLanguage.HINDI to listOf(
            Regex("""(विज्ञापन|ऐड).*[०-९0-9]+.*(सेकंड|सेकेंड|बाद|में)"""),
            Regex("""[०-९0-9]+\s*(सेकंड|सेकेंड)?\s*(में|बाद).*(विज्ञापन|छोड़|छोड़|स्किप)"""),
            Regex("""(विज्ञापन|ऐड).*(बाद|समाप्त\s*होने\s*पर)\s*वीडियो"""),
            Regex("""विज्ञापन.*(समाप्त|खत्म)\s*होगा""")
        )
    )

    // Strong Ad State patterns per language (Ad 1 of 2 family, video-after-ad, ad-will-end)
    private val STRONG_AD_REGEXES: Map<AppLanguage, List<Regex>> = mapOf(
        AppLanguage.ENGLISH to listOf(
            Regex("""(?i)^ad\s+\d+\s+of\s+\d+\b.*"""),
            Regex("""(?i)\bvideo\s+will\s+play\s+after\s+ad\b"""),
            Regex("""(?i)\byour\s+video\s+will\s+begin\s+in\b"""),
            Regex("""(?i)\bad\s+will\s+end\b"""),
            Regex("""(?i)\bad\s+ends\s+in\b""")
        ) + COUNTDOWN_REGEXES[AppLanguage.ENGLISH].orEmpty(),
        AppLanguage.BANGLA to listOf(
            Regex("""বিজ্ঞাপন\s*[০-৯0-9]+\s*(এর|/|মধ্যে)\s*[০-৯0-9]+"""),
            Regex("""বিজ্ঞাপনের\s*পরে\s*ভিডিও\s*চলবে""")
        ) + COUNTDOWN_REGEXES[AppLanguage.BANGLA].orEmpty(),
        AppLanguage.HINDI to listOf(
            Regex("""विज्ञापन\s*[०-९0-9]+\s*(का|/|में)\s*[०-९0-9]+"""),
            Regex("""विज्ञापन\s*के\s*बाद\s*वीडियो\s*चलेगा""")
        ) + COUNTDOWN_REGEXES[AppLanguage.HINDI].orEmpty()
    )

    private enum class WeakCategory {
        AD_BADGE,
        SPONSORED,
        ADVERTISER_CTA,
        AD_VIEW_ID
    }

    private val WEAK_AD_BADGE_EXACT: Map<AppLanguage, Set<String>> = mapOf(
        AppLanguage.ENGLISH to setOf("ad", "advertisement"),
        AppLanguage.BANGLA to setOf("বিজ্ঞাপন", "অ্যাড"),
        AppLanguage.HINDI to setOf("विज्ञापन", "ऐड")
    )

    private val WEAK_SPONSORED_EXACT: Map<AppLanguage, Set<String>> = mapOf(
        AppLanguage.ENGLISH to setOf("sponsored"),
        AppLanguage.BANGLA to setOf("স্পনসর্ড", "স্পন্সরড", "পৃষ্ঠপোষকতায়"),
        AppLanguage.HINDI to setOf("प्रायोजित", "स्पॉन्सर्ड")
    )

    // Advertiser CTAs — Download / Install strictly EXCLUDED per spec
    private val WEAK_CTA_PHRASES: Map<AppLanguage, Set<String>> = mapOf(
        AppLanguage.ENGLISH to setOf(
            "visit advertiser", "learn more", "shop now", "get offer", "sign up"
        ),
        AppLanguage.BANGLA to setOf(
            "বিজ্ঞাপনদাতার সাইট দেখুন", "আরও জানুন", "এখনই কিনুন", "অফার নিন", "সাইন আপ করুন"
        ),
        AppLanguage.HINDI to setOf(
            "विज्ञापनदाता पर जाएं", "अधिक जानें", "अभी खरीदें", "ऑफ़र पाएं", "साइन अप करें"
        )
    )

    private val BARE_DIGITS_OR_TIMER = Regex("""^[০-৯०-९0-9]{1,2}(\s*(s|sec|সে\.|से\.))?$""")
    private val AD_BADGE_WITH_TIME = Regex("""(?i)^(ad|বিজ্ঞাপন|विज्ञापन)\s*[·•:\-]\s*[০-৯०-९0-9]+:[০-৯०-९0-9]{2}$""")

    /**
     * Language Priority Cascade order:
     * - Selected BANGLA  -> [BANGLA, ENGLISH, HINDI]
     * - Selected ENGLISH -> [ENGLISH, HINDI, BANGLA]
     * - Selected HINDI   -> [HINDI, ENGLISH, BANGLA]
     */
    fun buildCascadeOrder(selectedLanguage: AppLanguage, systemLocale: Locale = Locale.getDefault()): List<AppLanguage> {
        val concrete = selectedLanguage.resolveConcrete(systemLocale)
        return when (concrete) {
            AppLanguage.BANGLA -> listOf(AppLanguage.BANGLA, AppLanguage.ENGLISH, AppLanguage.HINDI)
            AppLanguage.ENGLISH -> listOf(AppLanguage.ENGLISH, AppLanguage.HINDI, AppLanguage.BANGLA)
            AppLanguage.HINDI -> listOf(AppLanguage.HINDI, AppLanguage.ENGLISH, AppLanguage.BANGLA)
            AppLanguage.SYSTEM -> listOf(AppLanguage.ENGLISH, AppLanguage.HINDI, AppLanguage.BANGLA)
        }
    }

    fun analyzeTree(
        nodes: List<NodeSnapshot>,
        selectedLanguage: AppLanguage,
        systemLocale: Locale = Locale.getDefault()
    ): ScanAnalysis {
        if (nodes.isEmpty()) {
            return ScanAnalysis(AdState.NONE, emptyList(), emptyList(), null, null)
        }
        val visibleNodes = nodes.filter { it.isVisibleToUser }
        if (visibleNodes.isEmpty()) {
            return ScanAnalysis(AdState.NONE, emptyList(), emptyList(), null, null)
        }

        val cascade = buildCascadeOrder(selectedLanguage, systemLocale)

        // 1) Separate Skip Detection rule set (Step 0 View ID -> Step 1..3 language cascade, early exit)
        val skipCandidate = findSkipCandidate(visibleNodes, cascade)

        // 2) Separate Ad Detection rule set (Step 0 structural IDs -> Step 1..3 language cascade, early exit)
        val adResult = detectAdState(visibleNodes, cascade, skipCandidate)

        return ScanAnalysis(
            adState = adResult.adState,
            strongSignals = adResult.strongSignals,
            weakSignals = adResult.weakSignals,
            skipCandidate = skipCandidate,
            matchedLanguageStep = skipCandidate?.languageStep ?: adResult.matchedLanguageStep
        )
    }

    private fun findSkipCandidate(
        visibleNodes: List<NodeSnapshot>,
        cascade: List<AppLanguage>
    ): SkipCandidate? {
        // STEP 0: View ID first (language-neutral)
        var countdownHit: SkipCandidate? = null
        for (node in visibleNodes) {
            val id = node.viewIdResourceName.lowercase(Locale.ROOT)
            if (id.isNotEmpty() && SKIP_VIEW_ID_SUBSTRINGS.any { id.contains(it) }) {
                val combined = bestLabel(node)
                if (isBlacklisted(combined)) continue
                if (isCountdownText(combined, cascade) || isBareCountdownDigit(combined)) {
                    if (countdownHit == null) {
                        countdownHit = SkipCandidate(
                            nodeIndex = node.nodeIndex,
                            matchedBy = "viewId-countdown:$id",
                            isCountdownOnly = true,
                            languageStep = "STEP0_VIEW_ID"
                        )
                    }
                    continue
                }
                // Plain Skip or icon-only skip_ad_button that is enabled + visible + valid bounds
                if (node.isEnabled && node.hasValidBounds) {
                    return SkipCandidate(
                        nodeIndex = node.nodeIndex,
                        matchedBy = "viewId:$id",
                        isCountdownOnly = false,
                        languageStep = "STEP0_VIEW_ID"
                    )
                }
            }
        }

        // STEPS 1..N: Language priority cascade with EARLY EXIT
        for ((idx, lang) in cascade.withIndex()) {
            val stepTag = "STEP${idx + 1}_${lang.name}"
            val aliases = SKIP_TEXT_ALIASES[lang].orEmpty()
            for (node in visibleNodes) {
                val labels = candidateLabels(node)
                if (labels.isEmpty()) continue
                if (labels.any { isBlacklisted(it) }) continue

                // If countdown text matches this language, mark countdown-only (NO click)
                if (labels.any { isCountdownForLanguage(it, lang) }) {
                    if (countdownHit == null) {
                        countdownHit = SkipCandidate(
                            nodeIndex = node.nodeIndex,
                            matchedBy = "text-countdown:${labels.first()}",
                            isCountdownOnly = true,
                            languageStep = stepTag
                        )
                    }
                    continue
                }

                // Exact alias match for active plain Skip button
                val matched = labels.firstOrNull { label ->
                    val normalized = normalize(label)
                    aliases.any { alias -> normalized == normalize(alias) }
                }
                if (matched != null && node.isEnabled && node.hasValidBounds) {
                    // Early exit on first matching language step!
                    return SkipCandidate(
                        nodeIndex = node.nodeIndex,
                        matchedBy = "text:$matched",
                        isCountdownOnly = false,
                        languageStep = stepTag
                    )
                }
            }
        }

        return countdownHit
    }

    private data class AdDetectionResult(
        val adState: AdState,
        val strongSignals: List<String>,
        val weakSignals: List<String>,
        val matchedLanguageStep: String?
    )

    private fun detectAdState(
        visibleNodes: List<NodeSnapshot>,
        cascade: List<AppLanguage>,
        skipCandidate: SkipCandidate?
    ): AdDetectionResult {
        val strongSignals = mutableListOf<String>()
        val weakSignals = mutableListOf<String>()
        val weakCategories = mutableSetOf<WeakCategory>()

        // If Skip Detection already found a Skip button or Skip countdown, that is a strong ad signal.
        if (skipCandidate != null) {
            strongSignals.add("skip-node:${skipCandidate.matchedBy}")
            return AdDetectionResult(
                adState = AdState.AD_ACTIVE,
                strongSignals = strongSignals,
                weakSignals = weakSignals,
                matchedLanguageStep = skipCandidate.languageStep
            )
        }

        // STEP 0: Language-neutral structural / View ID signals
        for (node in visibleNodes) {
            val id = node.viewIdResourceName.lowercase(Locale.ROOT)
            if (id.isNotEmpty()) {
                if (STRONG_AD_VIEW_ID_SUBSTRINGS.any { id.contains(it) }) {
                    val label = bestLabel(node)
                    // Only treat container ID as strong if it has visible text/bounds
                    if (node.hasValidBounds && (label.isNotBlank() || !id.contains("container"))) {
                        strongSignals.add("viewId:$id")
                        return AdDetectionResult(
                            adState = AdState.AD_ACTIVE,
                            strongSignals = strongSignals,
                            weakSignals = weakSignals,
                            matchedLanguageStep = "STEP0_VIEW_ID"
                        )
                    }
                }
                if (WEAK_AD_VIEW_ID_SUBSTRINGS.any { id.contains(it) } && node.hasValidBounds) {
                    weakCategories.add(WeakCategory.AD_VIEW_ID)
                    weakSignals.add("weakId:$id")
                }
            }
            // Language-neutral "Ad · 0:15" badge+timer combined pattern
            for (label in candidateLabels(node)) {
                if (AD_BADGE_WITH_TIME.matches(label.trim())) {
                    strongSignals.add("badge-timer:$label")
                    return AdDetectionResult(
                        adState = AdState.AD_ACTIVE,
                        strongSignals = strongSignals,
                        weakSignals = weakSignals,
                        matchedLanguageStep = "STEP0_STRUCTURAL"
                    )
                }
            }
        }

        // STEPS 1..N: Language priority cascade with EARLY EXIT as soon as AD_ACTIVE is reached
        for ((idx, lang) in cascade.withIndex()) {
            val stepTag = "STEP${idx + 1}_${lang.name}"
            val strongRegexes = STRONG_AD_REGEXES[lang].orEmpty()
            val badges = WEAK_AD_BADGE_EXACT[lang].orEmpty()
            val sponsoredWords = WEAK_SPONSORED_EXACT[lang].orEmpty()
            val ctas = WEAK_CTA_PHRASES[lang].orEmpty()

            for (node in visibleNodes) {
                val labels = candidateLabels(node)
                for (rawLabel in labels) {
                    val trimmed = rawLabel.trim()
                    val normalized = normalize(trimmed)
                    if (normalized.isEmpty()) continue

                    // Check STRONG signals first
                    if (strongRegexes.any { it.containsMatchIn(trimmed) }) {
                        strongSignals.add("strong(${lang.code}):$trimmed")
                        return AdDetectionResult(
                            adState = AdState.AD_ACTIVE,
                            strongSignals = strongSignals,
                            weakSignals = weakSignals,
                            matchedLanguageStep = stepTag
                        )
                    }

                    // Check WEAK signals (grouped by independent category)
                    if (badges.any { normalize(it) == normalized }) {
                        weakCategories.add(WeakCategory.AD_BADGE)
                        weakSignals.add("badge(${lang.code}):$trimmed")
                    } else if (sponsoredWords.any { normalize(it) == normalized }) {
                        weakCategories.add(WeakCategory.SPONSORED)
                        weakSignals.add("sponsored(${lang.code}):$trimmed")
                    } else if (ctas.any { normalize(it) == normalized }) {
                        weakCategories.add(WeakCategory.ADVERTISER_CTA)
                        weakSignals.add("cta(${lang.code}):$trimmed")
                    }

                    // >= 2 independent weak signals confirm AD_ACTIVE (early exit!)
                    if (weakCategories.size >= 2) {
                        return AdDetectionResult(
                            adState = AdState.AD_ACTIVE,
                            strongSignals = strongSignals,
                            weakSignals = weakSignals,
                            matchedLanguageStep = stepTag
                        )
                    }
                }
            }
        }

        // CRITICAL FIX: Sponsored ALONE -> OVERLAY_SPONSORED (never mutes or announces)
        val finalState = when {
            weakCategories.size >= 2 -> AdState.AD_ACTIVE
            weakCategories == setOf(WeakCategory.SPONSORED) -> AdState.OVERLAY_SPONSORED
            else -> AdState.NONE
        }

        return AdDetectionResult(
            adState = finalState,
            strongSignals = strongSignals,
            weakSignals = weakSignals,
            matchedLanguageStep = null
        )
    }

    internal fun isBlacklisted(raw: String): Boolean {
        val norm = normalize(raw)
        if (norm.isEmpty()) return false
        if (norm in BLACKLIST_EXACT_OR_TOKEN) return true
        // Reject explicit playback/nav controls containing blacklisted phrases
        val forbiddenPrefixes = listOf(
            "subscribe", "like ", "dislike ", "next ", "play ", "pause ", "share ",
            "download", "install", "skip 10", "skip forward", "skip back"
        )
        return forbiddenPrefixes.any { norm.startsWith(it) }
    }

    internal fun isCountdownText(raw: String, cascade: List<AppLanguage>): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        return cascade.any { isCountdownForLanguage(trimmed, it) }
    }

    private fun isCountdownForLanguage(trimmed: String, lang: AppLanguage): Boolean {
        return COUNTDOWN_REGEXES[lang].orEmpty().any { it.containsMatchIn(trimmed) }
    }

    private fun isBareCountdownDigit(raw: String): Boolean {
        return BARE_DIGITS_OR_TIMER.matches(raw.trim())
    }

    private fun candidateLabels(node: NodeSnapshot): List<String> {
        val list = ArrayList<String>(2)
        if (node.text.isNotBlank()) list.add(node.text.trim())
        if (node.contentDescription.isNotBlank() && node.contentDescription.trim() != node.text.trim()) {
            list.add(node.contentDescription.trim())
        }
        return list
    }

    private fun bestLabel(node: NodeSnapshot): String {
        return when {
            node.text.isNotBlank() -> node.text.trim()
            node.contentDescription.isNotBlank() -> node.contentDescription.trim()
            else -> ""
        }
    }

    private fun normalize(input: String): String {
        return input.trim()
            .replace(Regex("""\s+"""), " ")
            .lowercase(Locale.ROOT)
    }
}
