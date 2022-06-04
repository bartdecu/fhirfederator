package ca.uhn.fhir.federator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

@Interceptor
public class FederatorInterceptor {

    public static final String PROCESSING = Constants.OO_INFOSTATUS_PROCESSING;
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FederatorInterceptor.class);

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
    public void doFederation2(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse,
            RequestDetails theRequestDetails, ServletRequestDetails theServletRequestDetails) throws ServletException {
        // add list of parameters to do nothing
        // _getpages, _getpagesoffset
        if (theRequestDetails.getParameters().get(Constants.PARAM_PAGINGACTION) != null) {
            return;
        }

        theRequestDetails.setOperation("$doFederation");
        theRequestDetails.setResource(null);
        theRequestDetails.setResourceName(null);
        ourLog.error("doFederation2");
    }

}
