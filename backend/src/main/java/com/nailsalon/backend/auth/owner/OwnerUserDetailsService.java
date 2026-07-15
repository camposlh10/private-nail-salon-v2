package com.nailsalon.backend.auth.owner;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class OwnerUserDetailsService implements UserDetailsService {

	private final OwnerUserRepository owners;

	public OwnerUserDetailsService(OwnerUserRepository owners) {
		this.owners = owners;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		OwnerUser owner = owners.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("No owner with email " + email));
		return User.builder()
				.username(owner.getEmail())
				.password(owner.getPasswordHash())
				.disabled(owner.getStatus() != OwnerUser.Status.ACTIVE)
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_OWNER")))
				.build();
	}
}
