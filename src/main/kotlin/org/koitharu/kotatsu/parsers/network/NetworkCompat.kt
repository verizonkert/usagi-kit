@file:Suppress("unused", "DEPRECATION")

package org.koitharu.kotatsu.parsers.network

@Deprecated("Legacy Kotatsu class. Use tsuki.network.WebClient instead.", ReplaceWith("WebClient", "tsuki.network.WebClient"))
public interface WebClient : tsuki.network.WebClient

@Deprecated("Legacy Kotatsu class. Use tsuki.network.OkHttpWebClient instead.", ReplaceWith("OkHttpWebClient", "tsuki.network.OkHttpWebClient"))
public typealias OkHttpWebClient = tsuki.network.OkHttpWebClient

@Deprecated("Legacy Kotatsu class. Use tsuki.network.CloudFlareHelper instead.", ReplaceWith("CloudFlareHelper", "tsuki.network.CloudFlareHelper"))
public typealias CloudFlareHelper = tsuki.network.CloudFlareHelper

@Deprecated("Legacy Kotatsu class. Use tsuki.network.CommonHeaders instead.", ReplaceWith("CommonHeaders", "tsuki.network.CommonHeaders"))
public typealias CommonHeaders = tsuki.network.CommonHeaders

@Deprecated("Legacy Kotatsu class. Use tsuki.network.RateLimitHelper instead.", ReplaceWith("RateLimitHelper", "tsuki.network.RateLimitHelper"))
public typealias RateLimitHelper = tsuki.network.RateLimitHelper

@Deprecated("Legacy Kotatsu class. Use tsuki.network.UserAgents instead.", ReplaceWith("UserAgents", "tsuki.network.UserAgents"))
public typealias UserAgents = tsuki.network.UserAgents
