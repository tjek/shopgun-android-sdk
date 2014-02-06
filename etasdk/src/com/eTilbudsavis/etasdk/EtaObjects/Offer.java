package com.eTilbudsavis.etasdk.EtaObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eTilbudsavis.etasdk.Network.Request;
import com.eTilbudsavis.etasdk.Utils.EtaLog;
import com.eTilbudsavis.etasdk.Utils.Utils;

public class Offer extends EtaErnObject implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String TAG = "Offer";
	
	/** Sort a list by popularity in ascending order. (smallest to largest) */
	public static final String SORT_POPULARITY = Request.Sort.POPULARITY;

	/** Sort a list by popularity in descending order. (largest to smallest)*/
	public static final String SORT_POPULARITY_DESC = Request.Sort.POPULARITY_DESC;

	/** Sort a list by distance in ascending order. (smallest to largest) */
	public static final String SORT_DISTANCE = Request.Sort.DISTANCE;

	/** Sort a list by distance in descending order. (largest to smallest)*/
	public static final String SORT_DISTANCE_DESC = Request.Sort.DISTANCE_DESC;

	/** Sort a list by page (in catalog) in ascending order. (smallest to largest) */
	public static final String SORT_PAGE = Request.Sort.PAGE;

	/** Sort a list by page (in catalog) in descending order. (largest to smallest)*/
	public static final String SORT_PAGE_DESC = Request.Sort.PAGE_DESC;

	/** Sort a list by created in ascending order. (smallest to largest) */
	public static final String SORT_CREATED = Request.Sort.CREATED;

	/** Sort a list by created in ascending order. (smallest to largest) */
	public static final String SORT_CREATED_DESC = Request.Sort.CREATED_DESC;

	/** Parameter for getting a list of specific catalog id's */
	public static final String FILTER_CATALOG_IDS = Request.Param.FILTER_CATALOG_IDS;

	/** Parameter for posting a list of store id's to publish the catalog in */
	public static final String FILTER_STORE_IDS = Request.Param.FILTER_STORE_IDS;

	/** Parameter for posting a list of store id's to publish the catalog in */
	public static final String FILTER_DEALER_IDS = Request.Param.FILTER_DEALER_IDS;

	/** String identifying the query parameter */
	public static final String PARAM_QUERY = Request.Param.QUERY;
	
	/** Endpoint for offer list resource */
	public static final String ENDPOINT_LIST = Request.Endpoint.OFFER_LIST;

	/** Endpoint for a single offer resource */
	public static final String ENDPOINT_ID = Request.Endpoint.OFFER_ID;

	/** Endpoint for searching offers */
	public static final String ENDPOINT_SEARCH = Request.Endpoint.OFFER_SEARCH;
	
	// From JSON blob
	private String mHeading;
	private String mDescription;
	private int mCatalogPage = 0;
	private Pricing mPricing;
	private Quantity mQuantity;
	private Images mImages;
	private Links mLinks;
	private Date mRunFrom = null;
	private Date mRunTill = null;
	private String mDealerUrl;
	private String mDealerId;
	private String mStoreUrl;
	private String mStoreId;
	private String mCatalogUrl;
	private String mCatalogId;
	
	// Other stuff
	private Catalog mCatalog;
	private Dealer mDealer;
	private Store mStore;

	public Offer() {
		
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Offer> fromJSON(JSONArray offers) {
		ArrayList<Offer> list = new ArrayList<Offer>();
		try {
			for (int i = 0 ; i < offers.length() ; i++ )
				list.add(Offer.fromJSON((JSONObject) offers.get(i)));
			
		} catch (JSONException e) {
			EtaLog.d(TAG, e);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static Offer fromJSON(JSONObject offer) {
		return fromJSON(new Offer(), offer);
	}

	private static Offer fromJSON(Offer o, JSONObject offer) {
		if (o == null) o = new Offer();
		if (offer == null) return o;

		try {
			o.setId(offer.getString(S_ID));
			o.setErn(offer.getString(S_ERN));
			o.setHeading(offer.getString(S_HEADING));
			o.setDescription(getJsonString(offer, S_DESCRIPTION));
			o.setCatalogPage(jsonToInt(offer, S_CATALOG_PAGE, 0));
			o.setPricing(Pricing.fromJSON(offer.getJSONObject(S_PRICING)));
			o.setQuantity(Quantity.fromJSON(offer.getJSONObject(S_QUANTITY)));
			o.setImages(Images.fromJSON(offer.getJSONObject(S_IMAGES)));
			o.setLinks(Links.fromJSON(offer.getJSONObject(S_LINKS)));
			Date runFrom = Utils.parseDate(getJsonString(offer, S_RUN_FROM));
			o.setRunFrom(runFrom);
			Date runTill = Utils.parseDate(getJsonString(offer, S_RUN_TILL));
			o.setRunTill(runTill);
			o.setDealerUrl(getJsonString(offer, S_DEALER_URL));
			o.setDealerId(getJsonString(offer, S_DEALER_ID));
			o.setStoreUrl(getJsonString(offer, S_STORE_URL));
			o.setStoreId(getJsonString(offer, S_STORE_ID));
			o.setCatalogUrl(getJsonString(offer, S_CATALOG_URL));
			o.setCatalogId(getJsonString(offer, S_CATALOG_ID));
		} catch (JSONException e) {
			EtaLog.d(TAG, e);
		}
		return o;
	}

	public JSONObject toJSON() {
		return toJSON(this);
	}

	public static JSONObject toJSON(Offer offer) {
		JSONObject o = new JSONObject();
		try {
			o.put(S_ID, offer.getId());
			o.put(S_ERN, offer.getErn());
			o.put(S_HEADING, offer.getHeading());
			o.put(S_DESCRIPTION, offer.getDescription());
			o.put(S_CATALOG_PAGE, offer.getCatalogPage());
			o.put(S_PRICING, offer.getPricing() == null ? null : offer.getPricing().toJSON());
			o.put(S_QUANTITY, offer.getQuantity() == null ? null : offer.getQuantity().toJSON());
			o.put(S_IMAGES, offer.getImages() == null ? null : offer.getImages().toJSON());
			o.put(S_LINKS, offer.getLinks() == null ? null : offer.getLinks().toJSON());
			o.put(S_RUN_FROM, Utils.formatDate(offer.getRunFrom()));
			o.put(S_RUN_TILL, Utils.formatDate(offer.getRunTill()));
			o.put(S_DEALER_URL, offer.getDealerUrl());
			o.put(S_DEALER_ID, offer.getDealerId());
			o.put(S_STORE_URL, offer.getStoreUrl());
			o.put(S_STORE_ID, offer.getStoreId());
			o.put(S_CATALOG_URL, offer.getCatalogUrl());
			o.put(S_CATALOG_ID, offer.getCatalogId());
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public String getHeading() {
		return mHeading;
	}

	public Offer setHeading(String heading) {
		mHeading = heading;
		return this;
	}

	public String getDescription() {
		return mDescription;
	}

	public Offer setDescription(String description) {
		mDescription = description;
		return this;
	}

	public int getCatalogPage() {
		return mCatalogPage;
	}

	public Offer setCatalogPage(int catalogPage) {
		mCatalogPage = catalogPage;
		return this;
	}

	public Pricing getPricing() {
		return mPricing;
	}

	public Offer setPricing(Pricing pricing) {
		mPricing = pricing;
		return this;
	}

	public Quantity getQuantity() {
		return mQuantity;
	}

	public Offer setQuantity(Quantity quantity) {
		mQuantity = quantity;
		return this;
	}

	public Images getImages() {
		return mImages;
	}

	public Offer setImages(Images images) {
		mImages = images;
		return this;
	}

	public Links getLinks() {
		return mLinks;
	}

	public Offer setLinks(Links links) {
		mLinks = links;
		return this;
	}

	public Date getRunFrom() {
		return mRunFrom;
	}

	public Offer setRunFrom(Date time) {
		time.setTime(1000 * (time.getTime()/ 1000));
		mRunFrom = time;
		return this;
	}

	public Date getRunTill() {
		return mRunTill;
	}

	public Offer setRunTill(Date time) {
		time.setTime(1000 * (time.getTime()/ 1000));
		mRunTill = time;
		return this;
	}

	public String getDealerUrl() {
		return mDealerUrl;
	}

	public Offer setDealerUrl(String url) {
		mDealerUrl = url;
		return this;
	}

	public String getDealerId() {
		return mDealerId;
	}

	public Offer setDealerId(String dealerId) {
		mDealerId = dealerId;
		return this;
	}

	public String getStoreUrl() {
		return mStoreUrl;
	}

	public Offer setStoreUrl(String url) {
		mStoreUrl = url;
		return this;
	}

	public String getStoreId() {
		return mStoreId;
	}

	public Offer setStoreId(String storeId) {
		mStoreId = storeId;
		return this;
	}

	public String getCatalogUrl() {
		return mCatalogUrl;
	}

	public Offer setCatalogUrl(String url) {
		mCatalogUrl = url;
		return this;
	}

	public String getCatalogId() {
		return mCatalogId;
	}

	public Offer setCatalogId(String catalogId) {
		mCatalogId = catalogId;
		return this;
	}

	public Catalog getCatalog() {
		return mCatalog;
	}

	public Offer setCatalog(Catalog catalog) {
		mCatalog = catalog;
		return this;
	}

	public Dealer getDealer() {
		return mDealer;
	}

	public Offer setDealer(Dealer dealer) {
		mDealer = dealer;
		return this;
	}

	public Store getStore() {
		return mStore;
	}

	public Offer setStore(Store store) {
		mStore = store;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (!(obj instanceof Offer))
			return false;

		Offer o = (Offer)obj;
		return stringCompare(mId, o.getId()) &&
				stringCompare(mErn, o.getErn()) &&
				stringCompare(mHeading, o.getHeading()) &&
				stringCompare(mDescription, o.getDescription()) &&
				mCatalogPage == o.getCatalogPage() &&
				mPricing == null ? o.getPricing() == null : mPricing.equals(o.getPricing()) &&
				mQuantity == null ? o.getQuantity() == null : mQuantity.equals(o.getQuantity()) &&
				mImages == null ? o.getImages() == null : mImages.equals(o.getImages()) &&
				mLinks == null ? o.getLinks() == null : mLinks.equals(o.getLinks()) &&
				mRunFrom == o.getRunFrom() &&
				mRunTill == o.getRunTill() &&
				stringCompare(mDealerUrl, o.getDealerUrl()) &&
				stringCompare(mDealerId, o.getDealerId()) &&
				stringCompare(mStoreUrl, o.getStoreUrl()) &&
				stringCompare(mStoreId, o.getStoreId()) &&
				stringCompare(mCatalogUrl, o.getCatalogUrl()) &&
				stringCompare(mCatalogId, o.getCatalogId()) &&
				mCatalog == null ? o.getCatalog() == null : mCatalog.equals(o.getCatalog()) &&
				mDealer == null ? o.getDealer() == null : mDealer.equals(o.getDealer()) &&
				mStore == null ? o.getStore() == null : mStore.equals(o.getStore());
	}
	
	/**
	 * Returns a human readable string containing id, heading, dealer of the offer. <br>
	 * SimpleClassName[prop1=value, prop2=NestedObject[prop3=value]]
	 * @return <li> Offer digest as a string 
	 */
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean everything) {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("[")
		.append("heading=").append(mHeading)
		.append(", description=").append(mDescription)
		.append(", dealer=").append(mDealer == null ? mDealerId : mDealer.toString(everything))
		.append(", pricing=").append(mPricing == null ? null : mPricing.toString())
		.append(", runFrom=").append(Utils.formatDate(getRunFrom()))
		.append(", runTill=").append(Utils.formatDate(getRunTill()));
		
		if (everything) {
			sb.append(", ern=").append(mErn)
			.append(", catalogPage=").append(mCatalogPage)
			.append(", quantity=").append(mQuantity == null ? null : mQuantity.toString())
			.append(", images=").append(mImages == null ? null : mImages.toString())
			.append(", links=").append(mLinks == null ? null : mLinks.toString())
			.append(", store=").append(mStore == null ? mStoreId : mStore.toString())
			.append(", catalog=").append(mCatalog == null ? mCatalogId : mCatalog.toString(everything));
		}
		return sb.append("]").toString();
	}
	
}
