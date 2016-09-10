package org.sharedhealth.mci.web.repository;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientAuditLog;
import org.sharedhealth.mci.web.model.PatientUpdateLog;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.RepositoryConstants;
import org.sharedhealth.mci.web.util.TestUtil;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.util.Date;

import static org.junit.Assert.*;

public class PatientRepositoryIT extends BaseIntegrationTest {
    private PatientRepository patientRepository;
    private Mapper<Patient> patientMapper;
    private Mapper<PatientUpdateLog> patientUpdateLogMapper;
    private Mapper<PatientAuditLog> patientAuditLogMapper;

    private final String healthId = "HID";
    private final String givenName = "Bob the";
    private final String surName = "Builder";
    private final String gender = "M";
    private final Date dateOfBirth = DateUtil.parseDate("1995-07-01 00:00:00+0530");
    private final String countryCode = "050";
    private final String divisionId = "30";
    private final String districtId = "26";
    private final String upazilaId = "18";
    private final String cityId = "02";
    private final String urbanWardId = "01";
    private final String ruralWardId = "04";
    private final String addressLine = "Will Street";

    @Before
    public void setUp() throws Exception {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
        patientMapper = mappingManager.mapper(Patient.class);
        patientUpdateLogMapper = mappingManager.mapper(PatientUpdateLog.class);
        patientAuditLogMapper = mappingManager.mapper(PatientAuditLog.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldRetrievePatientByHealthID() throws Exception {
        Patient expectedPatient = preparePatientData();
        patientMapper.save(expectedPatient);

        Patient patient = patientRepository.findByHealthId(healthId);

        assertNotNull(patient);
        assertEquals(expectedPatient, patient);
    }

    @Test
    public void shouldCreatePatientInDatabase() throws Exception {
        Patient patient = preparePatientData();

        MCIResponse mciResponse = patientRepository.createPatient(patient);

        Patient byHealthId = patientMapper.get(patient.getHealthId());
        assertEquals(patient, byHealthId);
        assertEquals(patient.getHealthId(), mciResponse.getId());
        assertEquals(HttpStatus.SC_CREATED, mciResponse.getHttpStatus());

        PatientAuditLog patientAuditLog = patientAuditLogMapper.get(patient.getHealthId());
        assertNotNull(patientAuditLog);
        assertEquals(patient.getCreatedAt(), patientAuditLog.getEventId());
        assertNull(patientAuditLog.getApprovedBy());
        assertNull(patientAuditLog.getRequestedBy());

        PatientUpdateLog patientUpdateLog = patientUpdateLogMapper.get(DateUtil.getYearOf(patient.getCreatedAt()));
        assertNotNull(patientUpdateLog);
        assertEquals(patient.getCreatedAt(), patientUpdateLog.getEventId());
        assertEquals(patient.getHealthId(), patientUpdateLog.getHealthId());
        assertEquals(RepositoryConstants.EVENT_TYPE_CREATED, patientUpdateLog.getEventType());
        assertNull(patientUpdateLog.getApprovedBy());


    }

    private Patient preparePatientData() {
        Patient expectedPatient = new Patient();
        expectedPatient.setHealthId(healthId);
        expectedPatient.setGivenName(givenName);
        expectedPatient.setSurName(surName);
        expectedPatient.setGender(gender);
        expectedPatient.setDateOfBirth(dateOfBirth);
        expectedPatient.setCountryCode(countryCode);
        expectedPatient.setDivisionId(divisionId);
        expectedPatient.setDistrictId(districtId);
        expectedPatient.setUpazilaId(upazilaId);
        expectedPatient.setCityCorporationId(cityId);
        expectedPatient.setUnionOrUrbanWardId(urbanWardId);
        expectedPatient.setRuralWardId(ruralWardId);
        expectedPatient.setAddressLine(addressLine);
        expectedPatient.setCreatedAt(TimeUuidUtil.uuidForDate(new Date()));
        return expectedPatient;
    }
}