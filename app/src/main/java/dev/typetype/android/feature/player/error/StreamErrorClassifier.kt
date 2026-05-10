package dev.typetype.android.feature.player.error

private val MEMBER_ONLY_NEEDLES = listOf(
    "only available for members",
    "members-only",
    "member only",
    "requires membership",
    "premium content",
)

private const val GENERIC_EXTRACTOR_ERROR =
    "Error occurs when fetching the page. Try increase the loading timeout in Settings."

fun isMemberOnlyMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val value = message.lowercase()
    if (MEMBER_ONLY_NEEDLES.any { it in value }) return true
    if (message == GENERIC_EXTRACTOR_ERROR) return true
    return false
}

private val GEO_PATTERN = Regex("only available in (.+)$", RegexOption.IGNORE_CASE)

private val COUNTRY_NAME_TO_ISO = mapOf(
    "Afghanistan" to "AF", "Albania" to "AL", "Algeria" to "DZ", "Andorra" to "AD",
    "Angola" to "AO", "Argentina" to "AR", "Armenia" to "AM", "Australia" to "AU",
    "Austria" to "AT", "Azerbaijan" to "AZ", "Bahrain" to "BH", "Bangladesh" to "BD",
    "Belarus" to "BY", "Belgium" to "BE", "Bolivia" to "BO", "Bosnia and Herzegovina" to "BA",
    "Brazil" to "BR", "Bulgaria" to "BG", "Cambodia" to "KH", "Cameroon" to "CM",
    "Canada" to "CA", "Chile" to "CL", "China" to "CN", "Colombia" to "CO",
    "Croatia" to "HR", "Cuba" to "CU", "Cyprus" to "CY", "Czech Republic" to "CZ",
    "Czechia" to "CZ", "Denmark" to "DK", "Ecuador" to "EC", "Egypt" to "EG",
    "Estonia" to "EE", "Ethiopia" to "ET", "Finland" to "FI", "France" to "FR",
    "Georgia" to "GE", "Germany" to "DE", "Ghana" to "GH", "Greece" to "GR",
    "Guatemala" to "GT", "Honduras" to "HN", "Hong Kong" to "HK", "Hungary" to "HU",
    "Iceland" to "IS", "India" to "IN", "Indonesia" to "ID", "Iran" to "IR",
    "Iraq" to "IQ", "Ireland" to "IE", "Israel" to "IL", "Italy" to "IT",
    "Jamaica" to "JM", "Japan" to "JP", "Jordan" to "JO", "Kazakhstan" to "KZ",
    "Kenya" to "KE", "Kosovo" to "XK", "Kuwait" to "KW", "Kyrgyzstan" to "KG",
    "Laos" to "LA", "Latvia" to "LV", "Lebanon" to "LB", "Libya" to "LY",
    "Lithuania" to "LT", "Luxembourg" to "LU", "Malaysia" to "MY", "Malta" to "MT",
    "Mexico" to "MX", "Moldova" to "MD", "Mongolia" to "MN", "Montenegro" to "ME",
    "Morocco" to "MA", "Myanmar" to "MM", "Nepal" to "NP", "Netherlands" to "NL",
    "New Zealand" to "NZ", "Nicaragua" to "NI", "Nigeria" to "NG", "North Korea" to "KP",
    "North Macedonia" to "MK", "Norway" to "NO", "Oman" to "OM", "Pakistan" to "PK",
    "Palestine" to "PS", "Panama" to "PA", "Paraguay" to "PY", "Peru" to "PE",
    "Philippines" to "PH", "Poland" to "PL", "Portugal" to "PT", "Qatar" to "QA",
    "Romania" to "RO", "Russia" to "RU", "Saudi Arabia" to "SA", "Senegal" to "SN",
    "Serbia" to "RS", "Singapore" to "SG", "Slovakia" to "SK", "Slovenia" to "SI",
    "Somalia" to "SO", "South Africa" to "ZA", "South Korea" to "KR", "Spain" to "ES",
    "Sri Lanka" to "LK", "Sudan" to "SD", "Sweden" to "SE", "Switzerland" to "CH",
    "Syria" to "SY", "Taiwan" to "TW", "Tajikistan" to "TJ", "Tanzania" to "TZ",
    "Thailand" to "TH", "Tunisia" to "TN", "Turkey" to "TR", "Turkmenistan" to "TM",
    "Uganda" to "UG", "Ukraine" to "UA", "United Arab Emirates" to "AE",
    "United Kingdom" to "GB", "United States" to "US", "Uruguay" to "UY",
    "Uzbekistan" to "UZ", "Venezuela" to "VE", "Vietnam" to "VN", "Yemen" to "YE",
    "Zimbabwe" to "ZW",
)

fun parseGeoRestriction(message: String?): String? {
    if (message.isNullOrBlank()) return null
    val match = GEO_PATTERN.find(message) ?: return null
    val countryName = match.groupValues[1].trim()
    return COUNTRY_NAME_TO_ISO[countryName]
}

enum class StreamErrorKind { Generic, MemberOnly, GeoRestricted }

data class StreamErrorClass(
    val kind: StreamErrorKind,
    val countryCode: String? = null,
    val rawMessage: String?,
)

fun classifyStreamError(message: String?): StreamErrorClass {
    val safe = message?.takeIf { it.isNotBlank() }
    val geo = parseGeoRestriction(safe)
    if (geo != null) {
        return StreamErrorClass(StreamErrorKind.GeoRestricted, geo, rawMessage = safe)
    }
    if (isMemberOnlyMessage(safe)) {
        return StreamErrorClass(StreamErrorKind.MemberOnly, countryCode = null, rawMessage = safe)
    }
    return StreamErrorClass(StreamErrorKind.Generic, countryCode = null, rawMessage = safe)
}
