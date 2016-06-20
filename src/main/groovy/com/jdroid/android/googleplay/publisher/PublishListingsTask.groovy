package com.jdroid.android.googleplay.publisher

import com.google.api.client.util.Lists
import com.jdroid.java.utils.StringUtils

public class PublishListingsTask extends AbstractTask {

	public PublishListingsTask() {
		description = "Publish listings (feature/promo graphics, High resolution icon, screenshots, title, short and full descriptions) on Google Play"
	}

	@Override
	protected void onExecute(AppContext appContext) {

		List<LocaleListing> localeListings = Lists.newArrayList();
		for (String each : StringUtils.splitToCollectionWithCommaSeparator(appContext.getLocales())) {
			String[] split = each.split("-");
			String language = split[0];
			String country = "";
			if (split.length > 1) {
				country = split[1];
			}
			localeListings.add(new LocaleListing(new Locale(language, country), appContext.getListingPath()));
		}
		GooglePlayPublisher.updateListings(appContext, localeListings, new LocaleListing(null, appContext.getListingPath()));

		// GooglePlayPublisher.updateApk(appContext, appContext.getApkPath(), appContext.getTrackType(),
		// localeListings);
	}
}
