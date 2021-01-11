package com.flagsmith;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A client for Flagsmith API.
 */
public class FlagsmithClient {

    private BulletTrainConfig defaultConfig;
    private static final String AUTH_HEADER = "X-Environment-Key";
    private static final String ACCEPT_HEADER = "Accept";
    // an api key per environment
    private String apiKey;

    private FlagsmithClient() {
    }

    /**
     * Get a list of existing Features for the given environment
     *
     * @return a list of feature flags
     */
    public List<Flag> getFeatureFlags() {
        return getFeatureFlags(null);
    }

    /**
     * Get a list of existing Features for the given environment and user
     *
     * @param user a user in context
     * @return a list of feature flags
     */
    public List<Flag> getFeatureFlags(FeatureUser user) {
        HttpUrl.Builder urlBuilder;
        if (user == null) {
            urlBuilder = defaultConfig.flagsURI.newBuilder()
                    .addEncodedQueryParameter("page", "1");
        } else {
            urlBuilder = defaultConfig.flagsURI.newBuilder("")
                    .addEncodedPathSegment(user.getIdentifier());
        }

        Request request = new Request.Builder()
                .header(AUTH_HEADER, apiKey)
                .addHeader(ACCEPT_HEADER, "application/json")
                .url(urlBuilder.build())
                .build();

        Call call = defaultConfig.httpClient.newCall(request);
        List<Flag> featureFlags = new ArrayList<>();
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ObjectMapper mapper = MapperFactory.getMappper();
                featureFlags = Arrays.asList(mapper.readValue(response.body().string(),
                        Flag[].class));
            }
        } catch (IOException io) {
        }
        return featureFlags;
    }

    /**
     * Check if Feature flag exist and is enabled
     *
     * @param featureId an identifier for the feature
     * @return true if feature flag exist and enabled, false otherwise
     */
    public boolean hasFeatureFlag(String featureId) {
        List<Flag> featureFlags = getFeatureFlags();
        return hasFeatureFlagByName(featureId, featureFlags);
    }

    /**
     * Check if Feature flag exist and is enabled for given user
     *
     * @param featureId a unique feature name identifier
     * @param user      a user in context
     * @return true if feature flag exist and enabled, false otherwise
     */
    public boolean hasFeatureFlag(String featureId, FeatureUser user) {
        List<Flag> featureFlags = getFeatureFlags(user);
        return hasFeatureFlagByName(featureId, featureFlags);
    }

    /**
     * Check if Feature flag exist and is enabled in a FlagsAndTraits
     *
     * @param featureId a unique feature name identifier
     * @param flagsAndTraits flags and traits object
     * @return true if feature flag exist and enabled, false otherwise
     */
    public static boolean hasFeatureFlag(String featureId, FlagsAndTraits flagsAndTraits) {
        if (flagsAndTraits == null) {
            return false;
        }
        return hasFeatureFlagByName(featureId, flagsAndTraits.getFlags());
    }

    /**
     * Check if Feature flag exist and is enabled in a list of flags
     *
     * @param featureId a unique feature name identifier
     * @param featureFlags a list of flags
     * @return true if feature flag exist and enabled, false otherwise
     */
    private static boolean hasFeatureFlagByName(String featureId, List<Flag> featureFlags) {
        for (Flag flag : featureFlags) {
            if (flag.getFeature().getName().equals(featureId) && flag.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get Feature value (remote config) for given feature id.
     *
     * @param featureId a unique feature name identifier
     * @return a value for the Feature or null if feature does not exist
     */
    public String getFeatureFlagValue(String featureId) {
        List<Flag> featureFlags = getFeatureFlags();
        return getFeatureFlagValueByName(featureId, featureFlags);
    }

    /**
     * Get Feature value (remote config) for given feature id and user
     *
     * @param featureId a unique feature name identifier
     * @param user      a user in context
     * @return a value for the feature or null if does not exist
     */
    public String getFeatureFlagValue(String featureId, FeatureUser user) {
        List<Flag> featureFlags = getFeatureFlags(user);
        return getFeatureFlagValueByName(featureId, featureFlags);
    }

    /**
     * Get Feature value (remote config) for given feature id and user
     *
     * @param featureId a unique feature name identifier
     * @param flagsAndTraits flags and traits object
     * @return a value for the feature or null if does not exist
     */
    public static String getFeatureFlagValue(String featureId, FlagsAndTraits flagsAndTraits) {
        if (flagsAndTraits == null) {
            return null;
        }
        return getFeatureFlagValueByName(featureId, flagsAndTraits.getFlags());
    }

    /**
     * Get Feature value (remote config) for given feature id from a provided flag list.
     *
     * @param featureId a unique feature name identifier
     * @param featureFlags list of feature flags
     * @return a value for the Feature or null if feature does not exist
     */
    private static String getFeatureFlagValueByName(String featureId, List<Flag> featureFlags) {
        for (Flag flag : featureFlags) {
            if (flag.getFeature().getName().equals(featureId)) {
                return flag.getStateValue();
            }
        }

        return null;
    }

    /**
     * Get user Trait for given user identity and trait key.
     *
     * @param key  a unique user trait key
     * @param user a user in context
     * @return a Trait object or null if does not exist
     */
    public Trait getTrait(FeatureUser user, String key) {
        List<Trait> traits = getUserTraits(user);
        return getTraitByKey(key, traits);
    }

    /**
     * Get user Trait from a given FlagsAndTraits and trait key.
     *
     * @param key  a unique user trait key
     * @param flagsAndTraits flags and traits object
     * @return a Trait object or null if does not exist
     */
    public static Trait getTrait(FlagsAndTraits flagsAndTraits, String key) {
        if (flagsAndTraits == null) {
            return null;
        }
        return getTraitByKey(key, flagsAndTraits.getTraits());
    }

    /**
     * Get user Trait from a given list of traits and trait key.
     *
     * @param key  a unique user trait key
     * @param traits list of traits
     * @return a Trait object or null if does not exist
     */
    private static Trait getTraitByKey(String key, List<Trait> traits) {
        for (Trait trait : traits) {
            if (trait.getKey().equals(key)) {
                return trait;
            }
        }
        return null;
    }

    /**
     * Get a list of user Traits for user identity and trait keys
     *
     * @return a list of user Trait
     */
    public List<Trait> getTraits(FeatureUser user, String... keys) {
        List<Trait> traits = getUserTraits(user);
        return getTraitsByKeys(traits, keys);
    }

    /**
     * Get a list of user Traits for user identity and trait keys
     *
     * @return a list of user Trait
     */
    public static List<Trait> getTraits(FlagsAndTraits flagsAndTraits, String... keys) {
        if (flagsAndTraits == null) {
            return null;
        }
        return getTraitsByKeys(flagsAndTraits.getTraits(), keys);
    }

    /**
     * Get a list of user Traits from trait list and trait keys
     *
     * @return a list of user Trait
     */
    private static List<Trait> getTraitsByKeys(List<Trait> traits, String[] keys) {
        // if no keys provided return all the user traits
        if (keys == null || keys.length == 0) {
            return traits;
        }

        // otherwise filter on give user traits keys
        List<Trait> filteredTraits = new ArrayList<>();
        for (Trait trait : traits) {
            if (Arrays.asList(keys).contains(trait.getKey())) {
                filteredTraits.add(trait);
            }
        }
        return filteredTraits;
    }

    /**
     * Get a list of existing user Traits for the given environment and identity user
     *
     * @param user a user in context
     * @return a list of user Traits
     */
    private List<Trait> getUserTraits(FeatureUser user) {
        return getUserFlagsAndTraits(user).getTraits();
    }

    /**
     * Get a list of existing user Traits and Flags for the given environment and identity user
     *
     * @param user a user in context
     * @return a list of user Traits and Flags
     */
    public FlagsAndTraits getUserFlagsAndTraits(FeatureUser user) {
        HttpUrl url = defaultConfig.identitiesURI.newBuilder("")
                .addEncodedQueryParameter("identifier", user.getIdentifier())
                .build();

        Request request = new Request.Builder()
                .header(AUTH_HEADER, apiKey)
                .addHeader(ACCEPT_HEADER, "application/json")
                .url(url)
                .build();

        Call call = defaultConfig.httpClient.newCall(request);

        FlagsAndTraits flagsAndTraits = new FlagsAndTraits();
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ObjectMapper mapper = MapperFactory.getMappper();
                flagsAndTraits = mapper.readValue(response.body().string(), FlagsAndTraits.class);
            }
        } catch (IOException io) {
        }
        return flagsAndTraits;
    }

    /**
     * Update user Trait for given user and Trait details.
     *
     * @param toUpdate a user trait to update
     * @param user     a user in context
     * @return a Trait object or null if does not exist
     */
    public Trait updateTrait(FeatureUser user, Trait toUpdate) {
        return postUserTraits(user, toUpdate);
    }

    private Trait postUserTraits(FeatureUser user, Trait toUpdate) {
        HttpUrl url = defaultConfig.traitsURI;
        toUpdate.setIdentity(user);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, toUpdate.toString());

        Request request = new Request.Builder()
                .header(AUTH_HEADER, apiKey)
                .addHeader(ACCEPT_HEADER, "application/json")
                .post(body)
                .url(url)
                .build();

        Call call = defaultConfig.httpClient.newCall(request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ObjectMapper mapper = MapperFactory.getMappper();
                Trait trait = mapper.readValue(response.body().string(), Trait.class);

                return trait;
            }
        } catch (IOException io) {
        }
        return null;
    }


    public static FlagsmithClient.Builder newBuilder() {
        return new FlagsmithClient.Builder();
    }


    public static class Builder {
        private FlagsmithClient client;
        private BulletTrainConfig configuration = BulletTrainConfig.newBuilder().build();

        private Builder() {
            client = new FlagsmithClient();
        }

        /**
         * Set the environment API key
         *
         * @param apiKey the api key for environment
         * @return the Builder
         */
        public Builder setApiKey(String apiKey) {
            if (null == apiKey) {
                throw new IllegalArgumentException("Api key can not be null");
            } else {
                client.apiKey = apiKey;
                return this;
            }
        }

        /**
         * Override default BulletTrainConfig for BulletTrain API.
         *
         * @param config an BulletTrainConfig to override default one.
         * @return the Builder
         */
        public Builder withConfiguration(BulletTrainConfig config) {
            if (config != null) {
                this.configuration = config;
            }
            return this;
        }

        /**
         * Set the base URL for BulletTrain API, overriding default one.
         *
         * @param apiUrl the new base URI for the API.
         * @return the Builder
         */
        public Builder withApiUrl(String apiUrl) {
            if (apiUrl != null) {
                this.configuration = BulletTrainConfig.newBuilder()
                        .baseURI(apiUrl)
                        .build();
            }
            return this;
        }

        public FlagsmithClient build() {
            client.defaultConfig = this.configuration;
            return client;
        }
    }
}