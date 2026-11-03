@file:Suppress("unused", "DEPRECATION")

package org.koitharu.kotatsu.parsers.model

@Deprecated("Legacy Kotatsu class. Use tsuki.model.Manga instead.", ReplaceWith("Manga", "tsuki.model.Manga"))
public typealias Manga = tsuki.model.Manga

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaChapter instead.", ReplaceWith("MangaChapter", "tsuki.model.MangaChapter"))
public typealias MangaChapter = tsuki.model.MangaChapter

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaPage instead.", ReplaceWith("MangaPage", "tsuki.model.MangaPage"))
public typealias MangaPage = tsuki.model.MangaPage

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaSource instead.", ReplaceWith("MangaSource", "tsuki.model.MangaSource"))
public interface MangaSource : tsuki.model.MangaSource

@Deprecated("Legacy Kotatsu enum/interface. Use tsuki.model.MangaSource instead.")
public interface MangaParserSource : MangaSource

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaState instead.", ReplaceWith("MangaState", "tsuki.model.MangaState"))
public typealias MangaState = tsuki.model.MangaState

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaTag instead.", ReplaceWith("MangaTag", "tsuki.model.MangaTag"))
public typealias MangaTag = tsuki.model.MangaTag

@Deprecated("Legacy Kotatsu class. Use tsuki.model.SortOrder instead.", ReplaceWith("SortOrder", "tsuki.model.SortOrder"))
public typealias SortOrder = tsuki.model.SortOrder

@Deprecated("Legacy Kotatsu class. Use tsuki.model.ContentRating instead.", ReplaceWith("ContentRating", "tsuki.model.ContentRating"))
public typealias ContentRating = tsuki.model.ContentRating

@Deprecated("Legacy Kotatsu class. Use tsuki.model.ContentType instead.", ReplaceWith("ContentType", "tsuki.model.ContentType"))
public typealias ContentType = tsuki.model.ContentType

@Deprecated("Legacy Kotatsu class. Use tsuki.model.Demographic instead.", ReplaceWith("Demographic", "tsuki.model.Demographic"))
public typealias Demographic = tsuki.model.Demographic

@Deprecated("Legacy Kotatsu class. Use tsuki.model.Favicon instead.", ReplaceWith("Favicon", "tsuki.model.Favicon"))
public typealias Favicon = tsuki.model.Favicon

@Deprecated("Legacy Kotatsu class. Use tsuki.model.Favicons instead.", ReplaceWith("Favicons", "tsuki.model.Favicons"))
public typealias Favicons = tsuki.model.Favicons

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaListFilter instead.", ReplaceWith("MangaListFilter", "tsuki.model.MangaListFilter"))
public typealias MangaListFilter = tsuki.model.MangaListFilter

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaListFilterCapabilities instead.", ReplaceWith("MangaListFilterCapabilities", "tsuki.model.MangaListFilterCapabilities"))
public typealias MangaListFilterCapabilities = tsuki.model.MangaListFilterCapabilities

@Deprecated("Legacy Kotatsu class. Use tsuki.model.MangaListFilterOptions instead.", ReplaceWith("MangaListFilterOptions", "tsuki.model.MangaListFilterOptions"))
public typealias MangaListFilterOptions = tsuki.model.MangaListFilterOptions

@Deprecated("Legacy Kotatsu class. Use tsuki.model.WordSet instead.", ReplaceWith("WordSet", "tsuki.model.WordSet"))
public typealias WordSet = tsuki.model.WordSet
