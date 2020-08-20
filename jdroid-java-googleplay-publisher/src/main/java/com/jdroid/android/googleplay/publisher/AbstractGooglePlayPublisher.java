package com.jdroid.android.googleplay.publisher;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.jdroid.java.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;


public class AbstractGooglePlayPublisher {

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/**
	 * Performs all necessary setup steps for running requests against the API.
	 *
	 * @param appContext the {@link AppContext}
	 * @return the {@link AndroidPublisher} service
	 */
	protected static AndroidPublisher init(AppContext appContext) {

		if (StringUtils.isEmpty(appContext.getApplicationId())) {
			throw new RuntimeException("The application id is required");
		}

		try {

			if (HTTP_TRANSPORT == null) {
				HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			}

			// Authorization.
			HttpRequestInitializer credential = authorizeWithServiceAccount(appContext);
			credential = setHttpTimeout(appContext, credential);

			// Set up and return API client.
			AndroidPublisher.Builder builder = new AndroidPublisher.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), credential);
			builder.setApplicationName(appContext.getApplicationId());
			return builder.build();
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static HttpRequestInitializer setHttpTimeout(AppContext appContext, HttpRequestInitializer requestInitializer) {
		return new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest httpRequest) throws IOException {
				requestInitializer.initialize(httpRequest);
				httpRequest.setConnectTimeout(appContext.getConnectTimeout());
				httpRequest.setReadTimeout(appContext.getReadTimeout());
			}
		};
	}

	private static HttpRequestInitializer authorizeWithServiceAccount(AppContext appContext) {

		if (StringUtils.isEmpty(appContext.getPrivateKeyJsonFilePath())) {
			throw new RuntimeException("The private key json file path is required");
		}

		try {
			InputStream serviceAccountStream = new FileInputStream(appContext.getPrivateKeyJsonFilePath());
			GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream).createScoped(AndroidPublisherScopes.ANDROIDPUBLISHER);
			return new HttpCredentialsAdapter(credentials);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	protected static AppEdit createEdit(App app, AndroidPublisher.Edits edits) {
		try {
			// Create a new edit to make changes.
			AppEdit edit = edits.insert(app.getApplicationId(), null).execute();
			log(String.format("Created edit with id: %s", edit.getId()));
			return edit;
		} catch (GoogleJsonResponseException ex) {
			throw new RuntimeException(ex.getDetails() != null ? ex.getDetails().getMessage() : ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected static void commitEdit(App app, AndroidPublisher.Edits edits, AppEdit edit) {
		if (app.getAppContext().isDryRun()) {
			log(String.format("Dry run mode enabled. App edit with id %s NOT comitted", edit.getId()));
		} else {
			try {
				AppEdit appEdit = edits.commit(app.getApplicationId(), edit.getId()).execute();
				log(String.format("App edit with id %s has been comitted", appEdit.getId()));
			} catch (GoogleJsonResponseException ex) {
				throw new RuntimeException(ex.getDetails().getMessage(), ex);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	protected static void log(String message) {
		// TODO Use a logger
		System.out.println(message);
	}
}
