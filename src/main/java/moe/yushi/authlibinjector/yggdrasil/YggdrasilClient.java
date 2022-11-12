/*
 * Copyright (C) 2020  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package moe.yushi.authlibinjector.yggdrasil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static moe.yushi.authlibinjector.util.IOUtils.CONTENT_TYPE_JSON;
import static moe.yushi.authlibinjector.util.IOUtils.asString;
import static moe.yushi.authlibinjector.util.IOUtils.http;
import static moe.yushi.authlibinjector.util.IOUtils.newUncheckedIOException;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonArray;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonObject;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonString;
import static moe.yushi.authlibinjector.util.JsonUtils.parseJson;
import static moe.yushi.authlibinjector.util.Logging.log;
import static moe.yushi.authlibinjector.util.Logging.Level.DEBUG;
import static moe.yushi.authlibinjector.util.Logging.Level.WARNING;
import static moe.yushi.authlibinjector.util.UUIDUtils.fromUnsignedUUID;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import moe.yushi.authlibinjector.internal.org.json.simple.JSONArray;
import moe.yushi.authlibinjector.internal.org.json.simple.JSONObject;
import moe.yushi.authlibinjector.yggdrasil.GameProfile.PropertyValue;

public class YggdrasilClient {

	private YggdrasilAPIProvider[] apiProviders;
	private Proxy proxy;

	public YggdrasilClient(YggdrasilAPIProvider... apiProviders) {
		this(null, apiProviders);
	}

	public YggdrasilClient(Proxy proxy, YggdrasilAPIProvider... apiProviders) {
		this.apiProviders = apiProviders;
		this.proxy = proxy;
	}

	public Map<String, UUID> queryUUIDs(Set<String> names) throws UncheckedIOException {
		names = new HashSet<>(names);
		Map<String, UUID> result = new LinkedHashMap<>();
		for (YggdrasilAPIProvider apiProvider : apiProviders) {
			String responseText;
			try {
				responseText = asString(http("POST", apiProvider.queryUUIDsByNames(),
						JSONArray.toJSONString(names).getBytes(UTF_8), CONTENT_TYPE_JSON,
						proxy));
			} catch (IOException e) {
				log(WARNING, "Query UUIDs of " + names + " at [" + apiProvider + "], failed: " + e);
				continue;
			}
			log(DEBUG, "Query UUIDs of " + names + " at [" + apiProvider + "], response: " + responseText);
			if (responseText.isEmpty()) continue;
	
			for (Object rawProfile : asJsonArray(parseJson(responseText))) {
				try {
					JSONObject profile = asJsonObject(rawProfile);
					String name = asJsonString(profile.get("name"));
					result.put(name,
							parseUnsignedUUID(asJsonString(profile.get("id"))));
					names.remove(name);
				} catch (UncheckedIOException e) {
					continue;
				}
			}
		}
		return result;
	}

	public Optional<UUID> queryUUID(String name) throws UncheckedIOException {
		return Optional.ofNullable(queryUUIDs(singleton(name)).get(name));
	}

	public Optional<GameProfile> queryProfile(UUID uuid, boolean withSignature) throws UncheckedIOException {
		for (YggdrasilAPIProvider apiProvider : apiProviders) {
			String url = apiProvider.queryProfile(uuid);
			if (withSignature) {
				url += "?unsigned=false";
			}
			String responseText;
			try {
				responseText = asString(http("GET", url, proxy));
			} catch (IOException e) {
				log(WARNING, "Query profile of [" + uuid + "] at [" + apiProvider + "], failed: "+e);
				continue;
			}
			if (responseText.isEmpty()) {
				log(DEBUG, "Query profile of [" + uuid + "] at [" + apiProvider + "], not found");
				continue;
			}
			log(DEBUG, "Query profile of [" + uuid + "] at [" + apiProvider + "], response: " + responseText);
	
			try {
				return Optional.of(parseGameProfile(asJsonObject(parseJson(responseText))));
			} catch (UncheckedIOException e) {
				continue;
			}
		}
		return Optional.empty();
	}
	
	public Optional<GameProfile> hasJoined(String username, String serverId) {
		for (YggdrasilAPIProvider apiProvider : apiProviders) {
			String url = apiProvider.hasJoined(username, serverId);
			String responseText;
			try {
				responseText = asString(http("GET", url, proxy));
			} catch (IOException e) {
				log(WARNING, "Has joined of [" + username + " " + serverId + "] at [" + apiProvider + "], failed: "+e);
				continue;
			}
			log(DEBUG, "Has joined of [" + username + " " + serverId + "] at [" + apiProvider + "], response: "+responseText);
	
			try {
				return Optional.of(parseGameProfile(asJsonObject(parseJson(responseText))));
			} catch (UncheckedIOException e) {
				continue;
			}
		}
		return Optional.empty();
	}

	private GameProfile parseGameProfile(JSONObject json) {
		GameProfile profile = new GameProfile();
		profile.id = parseUnsignedUUID(asJsonString(json.get("id")));
		profile.name = asJsonString(json.get("name"));
		profile.properties = new LinkedHashMap<>();
		for (Object rawProperty : asJsonArray(json.get("properties"))) {
			JSONObject property = (JSONObject) rawProperty;
			PropertyValue entry = new PropertyValue();
			entry.value = asJsonString(property.get("value"));
			if (property.containsKey("signature")) {
				entry.signature = asJsonString(property.get("signature"));
			}
			profile.properties.put(asJsonString(property.get("name")), entry);
		}
		return profile;
	}

	private UUID parseUnsignedUUID(String uuid) throws UncheckedIOException {
		try {
			return fromUnsignedUUID(uuid);
		} catch (IllegalArgumentException e) {
			throw newUncheckedIOException(e.getMessage());
		}
	}
}
