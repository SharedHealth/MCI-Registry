package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.util.FileUtil;

import java.util.List;
import java.util.zip.DataFormatException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FhirContextHelper.parseResource;

public class FhirPatientValidatorTest {
    @Mock
    private MCIProperties mciProperties;

    private FhirPatientValidator fhirPatientValidator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        String path = this.getClass().getResource("/profiles/").getPath();
        when(mciProperties.getProfilesFolderPath()).thenReturn(path);
        fhirPatientValidator = new FhirPatientValidator(mciProperties);
    }

    @Test
    public void shouldValidateAPatientResource() throws Exception {
        Bundle bundle = createPatientBundleFromFile("patients/valid_patient_with_mandatory_fields.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(bundle);
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldFailIfNotAValidGender() throws Exception {
        Bundle bundle = createPatientBundleFromFile("patients/patient_with_invalid_gender.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(bundle);

        assertFalse(validationResult.isSuccessful());
        SingleValidationMessage message = validationResult.getMessages().get(0);
        assertEquals("/f:Patient/f:gender", message.getLocationString());
        assertEquals("The value provided is not in the value set http://hl7.org/fhir/ValueSet/administrative-gender (http://hl7.org/fhir/ValueSet/administrative-gender, and a code is required from this value set", message.getMessage());
    }

    @Test
    public void shouldUseMentionedProfileForValidation() throws Exception {
        Bundle bundle = createPatientBundleFromFile("patients/invalid_patient_for_custom_profile.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(bundle);

        assertFalse(validationResult.isSuccessful());
        assertTrue(containsError(validationResult.getMessages(), "/f:Patient", "Element '/f:Patient.name': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationResult.getMessages(), "/f:Patient", "Element '/f:Patient.gender': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationResult.getMessages(), "/f:Patient", "Element '/f:Patient.birthDate': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationResult.getMessages(), "/f:Patient", "Element '/f:Patient.address': minimum required = 1, but only found 0"));
    }

    private boolean containsError(List<SingleValidationMessage> messages, String location, String message) {
        return messages.stream().anyMatch(validationMessage -> validationMessage.getLocationString().equals(location)
                && validationMessage.getMessage().equals(message));
    }

    private Bundle createPatientBundleFromFile(String filePath) throws DataFormatException {
        return (Bundle) parseResource(FileUtil.asString(filePath));
    }
}