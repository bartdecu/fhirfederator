package ca.uhn.fhir.federator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

@Interceptor
public class FederatorInterceptor {

  public static final String PROCESSING = Constants.OO_INFOSTATUS_PROCESSING;
  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(FederatorInterceptor.class);

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
  public void doFederation2(
      HttpServletRequest theServletRequest,
      HttpServletResponse theServletResponse,
      RequestDetails theRequestDetails,
      ServletRequestDetails theServletRequestDetails)
      throws ServletException {
    if (theServletRequestDetails.getId() != null) {
      ourLog.info("This is a read URL: {}", theRequestDetails.getCompleteUrl());
      return;
    }
    // add list of parameters to do nothing
    // _getpages, _getpagesoffset
    if (theRequestDetails.getParameters().get(Constants.PARAM_PAGINGACTION) != null) {
      ourLog.info("This is a paging URL: {}", theRequestDetails.getCompleteUrl());
      return;
    }
    if ("metadata".equals(theRequestDetails.getOperation())) {
      ourLog.info("This is a metadata URL: {}", theRequestDetails.getCompleteUrl());
      return;
    }

    if (("_search".equals(theRequestDetails.getOperation())
            && theRequestDetails.getRequestType().name().equalsIgnoreCase("POST"))
        || (!StringUtils.isEmpty(theRequestDetails.getResourceName())
            && theRequestDetails.getRequestType().name().equalsIgnoreCase("GET"))) {

      theRequestDetails.setOperation("$doFederation");
      theRequestDetails.setResource(null);
      theRequestDetails.setResourceName(null);
      ourLog.error("doFederation2");
    }

    return;
  }
}
