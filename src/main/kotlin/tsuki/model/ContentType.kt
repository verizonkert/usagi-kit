package tsuki.model

/**
 * Categorization of publication format, regional medium, and art style for manga sources and titles.
 */
public enum class ContentType {

	/**
	 * Japanese manga, black-and-white serialization, or general graphic novels.
	 */
	MANGA,

	/**
	 * Korean webtoons or comics (typically full-color vertical scroll).
	 */
	MANHWA,

	/**
	 * Chinese manhua or webcomics.
	 */
	MANHUA,

	/**
	 * Dedicated adult/NSFW manga and doujinshi content.
	 */
	HENTAI,

	/**
	 * Western style comic books and graphic novels.
	 */
	COMICS,

	/**
	 * Written light novels, web novels, or text-heavy literature.
	 */
	NOVEL,

	/**
	 * Standalone single-chapter stories or self-contained works.
	 */
	ONE_SHOT,

	/**
	 * Self-published, fan-made, or indie derivative works.
	 */
	DOUJINSHI,

	/**
	 * Gallery collections, image sets, or photography/illustration albums.
	 */
	IMAGE_SET,

	/**
	 * Computer graphics or illustrations curated from professional/independent digital artists.
	 */
	ARTIST_CG,

	/**
	 * Computer graphics, illustrations, and event stills extracted from video games.
	 */
	GAME_CG,

	/**
	 * General fallback category for miscellaneous or unclassified content mediums.
	 */
	OTHER,
}

