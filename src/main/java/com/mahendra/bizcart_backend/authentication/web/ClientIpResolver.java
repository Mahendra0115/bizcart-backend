package com.mahendra.bizcart_backend.authentication.web;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {
	private final AuthenticationProperties properties;
	public ClientIpResolver(AuthenticationProperties properties) { this.properties = properties; }

	public String resolve(HttpServletRequest request) {
		String remote = request.getRemoteAddr();
		if (!isTrusted(remote)) return remote;
		String forwarded = request.getHeader("X-Forwarded-For");
		if (!StringUtils.hasText(forwarded)) return remote;
		String[] chain = forwarded.split(",");
		for (int i = chain.length - 1; i >= 0; i--) {
			String candidate = chain[i].trim();
			if (!isTrusted(candidate)) return candidate;
		}
		return chain[0].trim();
	}

	private boolean isTrusted(String address) {
		if (!StringUtils.hasText(address)) return false;
		return Arrays.stream(properties.getProxy().getTrustedProxies().split(","))
			.map(String::trim).filter(StringUtils::hasText).anyMatch(cidr -> matches(address, cidr));
	}

	private boolean matches(String address, String cidr) {
		try {
			if (!cidr.contains("/")) return InetAddress.getByName(address).equals(InetAddress.getByName(cidr));
			String[] parts = cidr.split("/");
			byte[] ip = InetAddress.getByName(address).getAddress();
			byte[] network = InetAddress.getByName(parts[0]).getAddress();
			if (ip.length != network.length) return false;
			int bits = Integer.parseInt(parts[1]);
			for (int i = 0; i < ip.length; i++) {
				int mask = bits >= 8 ? 255 : bits <= 0 ? 0 : 256 - (1 << (8 - bits));
				if ((ip[i] & mask) != (network[i] & mask)) return false;
				bits -= 8;
			}
			return true;
		} catch (Exception ignored) { return false; }
	}
}
