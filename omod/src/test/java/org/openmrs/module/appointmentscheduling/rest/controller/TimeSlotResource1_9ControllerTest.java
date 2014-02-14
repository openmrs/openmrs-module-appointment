package org.openmrs.module.appointmentscheduling.rest.controller;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

public class TimeSlotResource1_9ControllerTest extends MainResourceControllerTest {
	
	private AppointmentService appointmentService;
	
	@Before
	public void setup() throws Exception {
		appointmentService = Context.getService(AppointmentService.class);
		executeDataSet("standardWebAppointmentTestDataset.xml");
	}
	
	@Test
	public void shouldGetTimeSlotByUuid() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		SimpleObject result = deserialize(handle(req));
		
		Assert.assertNotNull(result);
		Assert.assertEquals(getUuid(), PropertyUtils.getProperty(result, "uuid"));
		Assert.assertTrue(PropertyUtils.getProperty(result, "startDate").toString().contains("2007-01-01T00:00:00.200-0500"));
		Assert.assertTrue(PropertyUtils.getProperty(result, "endDate").toString().contains("2007-01-01T01:00:00.000-0500"));
		Assert.assertEquals("Super User, Xanadu: 2007-01-01 00:00:00.2 - 2007-01-01 01:00:00.0",
		    PropertyUtils.getProperty(result, "display"));
		
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183599", Util.getByPath(result, "appointmentBlock/uuid"));
		Assert.assertEquals(true, PropertyUtils.getProperty(result, "voided"));
	}
	
	@Test
	public void shouldGetFullTimeSlotByUuid() throws Exception {
		
		MockHttpServletRequest req = newGetRequest(getURI() + "/" + getUuid(), new MainResourceControllerTest.Parameter(
		        RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION, RestConstants.REPRESENTATION_FULL));
		SimpleObject result = deserialize(handle(req));
		
		Assert.assertNotNull(result);
		Assert.assertEquals(getUuid(), PropertyUtils.getProperty(result, "uuid"));
		Assert.assertTrue(PropertyUtils.getProperty(result, "startDate").toString().contains("2007-01-01T00:00:00.200-0500"));
		Assert.assertTrue(PropertyUtils.getProperty(result, "endDate").toString().contains("2007-01-01T01:00:00.000-0500"));
		Assert.assertEquals("Super User, Xanadu: 2007-01-01 00:00:00.2 - 2007-01-01 01:00:00.0",
		    PropertyUtils.getProperty(result, "display"));
		
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183599", Util.getByPath(result, "appointmentBlock/uuid"));
		Assert.assertEquals(true, PropertyUtils.getProperty(result, "voided"));
		
	}
	
	@Test
	public void shouldCreateNewTimeSlot() throws Exception {
		
		int originalCount = 7;
		String json = "{ \"startDate\":\"2005-01-03T09:00:00.000-0500\", \"endDate\":\"2005-01-03T10:00:00.000-0500\", "
		        + "\"appointmentBlock\": \"759799ab-c9a5-435e-b671-77773ada7499\" }";
		
		MockHttpServletRequest req = request(RequestMethod.POST, getURI());
		req.setContent(json.getBytes());
		
		Object appt = deserialize(handle(req));
		Assert.assertNotNull(PropertyUtils.getProperty(appt, "uuid"));
		Assert.assertTrue(PropertyUtils.getProperty(appt, "startDate").toString().contains("2005-01-03T09:00:00"));
		Assert.assertTrue(PropertyUtils.getProperty(appt, "endDate").toString().contains("2005-01-03T10:00:00"));
		Assert.assertEquals("759799ab-c9a5-435e-b671-77773ada7499",
		    PropertyUtils.getProperty(PropertyUtils.getProperty(appt, "appointmentBlock"), "uuid"));
		Assert.assertEquals(originalCount + 1, appointmentService.getAllTimeSlots().size());
		
	}
	
	@Test
	public void shouldEditATimeSlot() throws Exception {
		
		String json = "{ \"endDate\":\"2005-01-03T11:00:00.000-0500\" }";
		MockHttpServletRequest req = request(RequestMethod.POST, getURI() + "/c0c579b0-8e59-401d-8a4a-976a0b183606");
		req.setContent(json.getBytes());
		handle(req);
		
		TimeSlot updated = appointmentService.getTimeSlotByUuid("c0c579b0-8e59-401d-8a4a-976a0b183606");
		Assert.assertNotNull(updated);
		Assert.assertEquals("Mon Jan 03 11:00:00 EST 2005", updated.getEndDate().toString());
		
	}
	
	@Test
	public void shouldVoidATimeSlot() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.DELETE, getURI() + "/c0c579b0-8e59-401d-8a4a-976a0b183607");
		req.addParameter("!purge", "");
		req.addParameter("reason", "really ridiculous random reason");
		handle(req);
		
		TimeSlot voided = appointmentService.getTimeSlotByUuid("c0c579b0-8e59-401d-8a4a-976a0b183607");
		Assert.assertTrue(voided.isVoided());
		Assert.assertEquals("really ridiculous random reason", voided.getVoidReason());
	}
	
	@Test
	public void shouldPurgeATimeSlot() throws Exception {
		
		int originalCount = 7;
		
		MockHttpServletRequest req = request(RequestMethod.DELETE, getURI() + "/c0c579b0-8e59-401d-8a4a-976a0b183607");
		req.addParameter("purge", "");
		req.addParameter("reason", "really ridiculous random reason");
		handle(req);
		
		Assert.assertNull(appointmentService.getTimeSlotByUuid("c0c579b0-8e59-401d-8a4a-976a0b183607"));
		Assert.assertEquals(originalCount - 1, appointmentService.getAllTimeSlots().size());
		
	}
	
	@Test
	public void shouldFindTimeSlotBasedOnAppointmentTypeAndFromDateExcludingVoided() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2006-06-01T00:00:00.000");
		req.addParameter("appointmentType", "759799ab-c9a5-435e-b671-77773ada74e4");
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(2, timeSlots.size());
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183607", timeSlots.get(0).get("uuid"));
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183610", timeSlots.get(1).get("uuid"));
	}
	
	@Test
	public void shouldFindTimeSlotBasedOnAppointmentTypeAndDateExcludingVoided() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2006-06-01T00:00:00.000");
		req.addParameter("toDate", "2008-01-25T00:00:00.000");
		req.addParameter("appointmentType", "759799ab-c9a5-435e-b671-77773ada74e4");
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(1, timeSlots.size());
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183607", timeSlots.get(0).get("uuid"));
		
	}
	
	@Test
	public void shouldFindTimeSlotBasedOnAppointmentTypeAndFromDateAndLocationExcludingVoided() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2001-06-01T00:00:00.000");
		req.addParameter("appointmentType", "759799ab-c9a5-435e-b671-77773ada74e4");
		req.addParameter("location", "9356400c-a5a2-4532-8f2b-2361b3446eb8");
		
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(2, timeSlots.size());
		;
		// note that the first time slot is full, so it is not returned here
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183605", timeSlots.get(0).get("uuid"));
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183607", timeSlots.get(1).get("uuid"));
		
	}
	
	@Test
	public void shouldFindTimeSlotBasedOnAppointmentTypeAndFromDateAndProviderExcludingVoided() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2001-06-01T00:00:00.000");
		req.addParameter("appointmentType", "759799ab-c9a5-435e-b671-77773ada74e4");
		req.addParameter("provider", "c0c54sd0-8e59-401d-8a4a-976a0b183599");
		
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(1, timeSlots.size());
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183610", timeSlots.get(0).get("uuid"));
	}
	
	@Test
	public void shouldFindTimeSlotUsingAllParametersExcludingVoided() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2006-06-01T00:00:00.000");
		req.addParameter("toDate", "2009-01-01T00:00:00.000");
		req.addParameter("appointmentType", "c0c579b0-8e59-401d-8a4a-976a0b183519"); // appt type #1   (all blocks)
		req.addParameter("provider", "c0c549b0-8e59-401d-8a4a-976a0b183599"); // provider #1    (blocks #1, #2)
		req.addParameter("location", "9356400c-a5a2-4532-8f2b-2361b3446eb8"); // location #3 (block #1)
		
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(1, timeSlots.size());
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183607", timeSlots.get(0).get("uuid"));
	}
	
	@Test
	public void shouldFindTimeSlotIncludingFullSlots() throws Exception {
		
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("fromDate", "2001-06-01T00:00:00.000");
		req.addParameter("appointmentType", "759799ab-c9a5-435e-b671-77773ada74e4");
		req.addParameter("location", "9356400c-a5a2-4532-8f2b-2361b3446eb8");
		req.addParameter("includeFull", "true");
		
		handle(req);
		
		List<Map<String, String>> timeSlots = (List<Map<String, String>>) deserialize(handle(req)).get("results");
		Assert.assertEquals(3, timeSlots.size());
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183604", timeSlots.get(0).get("uuid"));
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183605", timeSlots.get(1).get("uuid"));
		Assert.assertEquals("c0c579b0-8e59-401d-8a4a-976a0b183607", timeSlots.get(2).get("uuid"));
		
	}
	
	@Override
	public String getURI() {
		return AppointmentRestController.APPOINTMENT_SCHEDULING_REST_NAMESPACE + "/timeslot";
	}
	
	@Override
	public String getUuid() {
		return "c0c579b0-8e59-401d-8a4a-976a0b183606";
	}
	
	@Override
	public long getAllCount() {
		return 6;
	}
	
}