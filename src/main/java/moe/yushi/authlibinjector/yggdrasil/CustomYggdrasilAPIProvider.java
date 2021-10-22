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

import static moe.yushi.authlibinjector.util.UUIDUtils.toUnsignedUUID;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

public class CustomYggdrasilAPIProvider implements YggdrasilAPIProvider {

	private String apiRoot;

	public CustomYggdrasilAPIProvider(String root) {
		this.apiRoot = root;
	}

	@Override
	public String queryUUIDsByNames() {
		return apiRoot + "api/profiles/minecraft";
	}

	@Override
	public String queryProfile(UUID uuid) {
		return apiRoot + "sessionserver/session/minecraft/profile/" + toUnsignedUUID(uuid);
	}
	
	@Override
	public String hasJoined(String username, String serverId) {
		try {
			return apiRoot + "sessionserver/session/minecraft/hasJoined?username="+URLEncoder.encode(username, "UTF-8")+"&serverId="+URLEncoder.encode(serverId, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public String toString() {
		return apiRoot;
	}
}
