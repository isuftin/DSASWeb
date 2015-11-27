package gov.usgs.cida.dsas.rest.service.security;

import java.io.IOException;
import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class DynamicRolesLoginRedirectFeature extends RolesAllowedDynamicFeature {

	private static final Logger LOG = LoggerFactory.getLogger(DynamicRolesLoginRedirectFeature.class);

	@Override
	public void configure(final ResourceInfo resourceInfo, final FeatureContext configuration) {
		AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

		// DenyAll on the method take precedence over RolesAllowed and PermitAll
		if (am.isAnnotationPresent(DenyAll.class)) {
			configuration.register(new RolesAllowedPastLoginFilter());
			return;
		}

		// RolesAllowed on the method takes precedence over PermitAll
		RolesAllowed ra = am.getAnnotation(RolesAllowed.class);
		if (ra != null) {
			configuration.register(new RolesAllowedPastLoginFilter(ra.value()));
			return;
		}

		// PermitAll takes precedence over RolesAllowed on the class
		if (am.isAnnotationPresent(PermitAll.class)) {
			// Do nothing.
			return;
		}

        // DenyAll can't be attached to classes
		// RolesAllowed on the class takes precedence over PermitAll
		ra = resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class);
		if (ra != null) {
			configuration.register(new RolesAllowedPastLoginFilter(ra.value()));
		}
	}

	@Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
	private static class RolesAllowedPastLoginFilter implements ContainerRequestFilter {

		private final boolean denyAll;
		private final String[] rolesAllowed;

		RolesAllowedPastLoginFilter() {
			this.denyAll = true;
			this.rolesAllowed = null;
		}

		RolesAllowedPastLoginFilter(String[] rolesAllowed) {
			this.denyAll = false;
			this.rolesAllowed = (rolesAllowed != null) ? rolesAllowed : new String[]{};
		}

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			if (!denyAll) {
				for (String role : rolesAllowed) {
					if (requestContext.getSecurityContext().isUserInRole(role)) {
						return;
					}
				}
			}

			throw new ForbiddenException();
		}
	}
}
