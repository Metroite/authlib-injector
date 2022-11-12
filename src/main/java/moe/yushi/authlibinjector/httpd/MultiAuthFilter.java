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
package moe.yushi.authlibinjector.httpd;

import static moe.yushi.authlibinjector.util.IOUtils.CONTENT_TYPE_JSON;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.IHTTPSession;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.Response;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.Status;
import moe.yushi.authlibinjector.yggdrasil.GameProfile;
import moe.yushi.authlibinjector.yggdrasil.YggdrasilClient;
import moe.yushi.authlibinjector.yggdrasil.YggdrasilResponseBuilder;

public class MultiAuthFilter implements URLFilter {

	private YggdrasilClient client;

	public MultiAuthFilter(YggdrasilClient client) {
		this.client = client;
	}

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("sessionserver.mojang.com");
	}

	@Override
	public Optional<Response> handle(String domain, String path, IHTTPSession session) throws IOException {
		if (domain.equals("sessionserver.mojang.com") && path.equals("/session/minecraft/hasJoined") && session.getMethod().equals("GET")) {
			String username = getQuery(session, "username");
			String serverId = getQuery(session, "serverId");
			if (username == null || serverId == null) return Optional.empty();
			Optional<GameProfile> profile = client.hasJoined(username, serverId);
			if (profile.isPresent()) {
				return Optional.of(Response.newFixedLength(Status.OK, CONTENT_TYPE_JSON, YggdrasilResponseBuilder.queryProfile(profile.get(), true)));
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	private String getQuery(IHTTPSession session, String key) {
		if (!session.getParameters().containsKey(key)) return null;
		List<String> li = session.getParameters().get(key);
		if (li.isEmpty()) return null;
		return li.get(0);
	}

}
