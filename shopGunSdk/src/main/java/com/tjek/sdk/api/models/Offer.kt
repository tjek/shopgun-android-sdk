package com.tjek.sdk.api.models

import com.tjek.sdk.api.*

data class Offer(
    val id: Id = "",
    val heading: String = "",
    val description: String = "",
    val webshopUrl: String = "",
    val runDateRange: ClosedRange<ValidityDate> = distantPast()..distantFuture(),
    val visibleFrom: ValidityDate = distantPast(),
    val price: Float = 0f,
    val currency: String = "",
    val savings: Float = 0f,
    val pieceCount: ClosedRange<Float> = 1f..1f,
    val unitSize: ClosedRange<Float> = 0f..0f,
    val unitSymbol: QuantityUnit = QuantityUnit.Piece,
    val branding: Branding = Branding(),
    val businessId: Id = "",
    val storeId: Id = "",
    // info about the offer placement on the respective publication
    val publicationInfo: PublicationInfo = PublicationInfo(),
    val imageUrls: ImageUrls = ImageUrls()
)

data class PublicationInfo(
    val publicationId: Id = "",
    val pagedPublicationPage: Int = 0, // this is 0 for incito publications
    val incitoViewId: Id = ""
)
